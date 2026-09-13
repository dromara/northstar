package org.dromara.northstar.trader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.TradeAccount;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.LineTraderDTO;
import org.dromara.northstar.common.model.LineTraderDTO.State;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 画线交易计划执行器单元测试
 */
class LineTraderTest {

	private static final String CONTRACT_ID = "rb2501@SHFE@FUTURES@SIM";
	private static final String GATEWAY_ID = "testAcc";

	AccountManager accMgr = mock(AccountManager.class);
	MarketCenter mktCenter = mock(MarketCenter.class);
	IContract gatewayContract = mock(IContract.class);
	TradeAccount account = mock(TradeAccount.class);
	@SuppressWarnings("unchecked")
	Consumer<LineTraderDTO> callback = mock(Consumer.class);

	Contract contract = Contract.builder()
			.contractId(CONTRACT_ID)
			.name("螺纹钢2501")
			.priceTick(1)
			.build();

	@BeforeEach
	void prepare() {
		when(accMgr.get(any())).thenReturn(account);
		when(mktCenter.getContract(any(Identifier.class))).thenReturn(gatewayContract);
		when(gatewayContract.contract()).thenReturn(contract);
	}

	/**
	 * 忽略其他合约的行情
	 */
	@Test
	void shouldIgnoreTickOfOtherContract() {
		LineTraderDTO dto = plan("long", 5000D, 4900D, 5200D, State.STANDBY);
		LineTrader trader = traderOf(dto);
		Contract other = Contract.builder().contractId("au2506@SHFE@FUTURES@SIM").name("黄金2506").priceTick(0.02).build();

		trader.onTick(tick(other, 5010));
		trader.onTick(tick(other, 4990));

		verify(account, never()).submitOrder(any());
		assertThat(dto.getState()).isEqualTo(State.STANDBY);
	}

	/**
	 * 已完成的计划不再响应行情
	 */
	@Test
	void shouldIgnoreTickWhenCompleted() {
		LineTraderDTO dto = plan("long", 5000D, 4900D, 5200D, State.COMPLETED);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(5010));
		trader.onTick(tick(4990));

		verify(account, never()).submitOrder(any());
	}

	/**
	 * 行情向下穿越入场价时触发多头开仓
	 */
	@Test
	void shouldOpenLongWhenPriceCrossesEntry() {
		LineTraderDTO dto = plan("long", 5000D, 4900D, 5200D, State.STANDBY);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(5010));
		trader.onTick(tick(4990));	// 向下穿越5000，且 4900 < 4990 < 5200

		ArgumentCaptor<SubmitOrderReq> captor = ArgumentCaptor.forClass(SubmitOrderReq.class);
		verify(account).submitOrder(captor.capture());
		SubmitOrderReq req = captor.getValue();
		assertThat(req.gatewayId()).isEqualTo(GATEWAY_ID);
		assertThat(req.contract()).isEqualTo(contract);
		assertThat(req.direction()).isEqualTo(DirectionEnum.D_Buy);
		assertThat(req.offsetFlag()).isEqualTo(OffsetFlagEnum.OF_Open);
		assertThat(req.price()).isEqualTo(5000D);
		assertThat(req.volume()).isEqualTo(1);
		assertThat(dto.getState()).isEqualTo(State.RUNNING);
		verify(callback).accept(dto);
	}

	/**
	 * 行情向上穿越入场价时触发空头开仓
	 */
	@Test
	void shouldOpenShortWhenPriceCrossesEntry() {
		LineTraderDTO dto = plan("short", 5000D, 5100D, 4800D, State.STANDBY);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(4990));
		trader.onTick(tick(5010));	// 向上穿越5000，且 4800 < 5010 < 5100

		ArgumentCaptor<SubmitOrderReq> captor = ArgumentCaptor.forClass(SubmitOrderReq.class);
		verify(account).submitOrder(captor.capture());
		SubmitOrderReq req = captor.getValue();
		assertThat(req.direction()).isEqualTo(DirectionEnum.D_Sell);
		assertThat(req.offsetFlag()).isEqualTo(OffsetFlagEnum.OF_Open);
		assertThat(dto.getState()).isEqualTo(State.RUNNING);
	}

	/**
	 * 首个行情仅作为参照，不会触发入场
	 */
	@Test
	void shouldNotOpenOnFirstTick() {
		LineTraderDTO dto = plan("long", 5000D, 4900D, 5200D, State.STANDBY);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(4990));

		verify(account, never()).submitOrder(any());
		assertThat(dto.getState()).isEqualTo(State.STANDBY);
	}

	/**
	 * 行情未穿越入场价时不触发入场
	 */
	@Test
	void shouldNotOpenWithoutCross() {
		LineTraderDTO dto = plan("long", 5000D, 4900D, 5200D, State.STANDBY);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(5010));
		trader.onTick(tick(5005));

		verify(account, never()).submitOrder(any());
		assertThat(dto.getState()).isEqualTo(State.STANDBY);
	}

	/**
	 * 穿越入场价但已越过止损价时不入场（行情已失控）
	 */
	@Test
	void shouldNotOpenWhenPriceBeyondStopLoss() {
		LineTraderDTO dto = plan("long", 5000D, 4900D, 5200D, State.STANDBY);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(5010));
		trader.onTick(tick(4880));	// 穿越5000，但 4880 < 4900 止损价

		verify(account, never()).submitOrder(any());
		assertThat(dto.getState()).isEqualTo(State.STANDBY);
	}

	/**
	 * 无入场价且已持仓时，计划自动进入运行中
	 */
	@Test
	void shouldBecomeRunningWhenPositionExists() {
		givenPosition(PositionDirectionEnum.PD_Long, 1, 0);
		LineTraderDTO dto = plan("long", null, 4000D, 6000D, State.STANDBY);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(5000));

		assertThat(dto.getState()).isEqualTo(State.RUNNING);
		verify(callback).accept(dto);
		verify(account, never()).submitOrder(any());
	}

	/**
	 * 触发多头止盈时自动平仓
	 */
	@Test
	void shouldCloseLongOnTakeProfit() {
		givenPosition(PositionDirectionEnum.PD_Long, 2, 0);
		LineTraderDTO dto = plan("long", null, 4900D, 5100D, State.RUNNING);
		dto.setVolume(2);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(5100));	// 5100 >= 5100 触发止盈

		ArgumentCaptor<SubmitOrderReq> captor = ArgumentCaptor.forClass(SubmitOrderReq.class);
		verify(account).submitOrder(captor.capture());
		SubmitOrderReq req = captor.getValue();
		assertThat(req.direction()).isEqualTo(DirectionEnum.D_Sell);
		assertThat(req.offsetFlag()).isEqualTo(OffsetFlagEnum.OF_Close);
		assertThat(req.volume()).isEqualTo(2);
		assertThat(dto.getState()).isEqualTo(State.COMPLETED);
		verify(callback).accept(dto);
	}

	/**
	 * 触发多头止损时自动平仓
	 */
	@Test
	void shouldCloseLongOnStopLoss() {
		givenPosition(PositionDirectionEnum.PD_Long, 2, 0);
		LineTraderDTO dto = plan("long", null, 4900D, 5100D, State.RUNNING);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(4890));	// 4890 <= 4900 触发止损

		ArgumentCaptor<SubmitOrderReq> captor = ArgumentCaptor.forClass(SubmitOrderReq.class);
		verify(account).submitOrder(captor.capture());
		assertThat(captor.getValue().direction()).isEqualTo(DirectionEnum.D_Sell);
		assertThat(captor.getValue().offsetFlag()).isEqualTo(OffsetFlagEnum.OF_Close);
		assertThat(dto.getState()).isEqualTo(State.COMPLETED);
	}

	/**
	 * 触发空头止盈时自动平仓
	 */
	@Test
	void shouldCloseShortOnTakeProfit() {
		givenPosition(PositionDirectionEnum.PD_Short, 2, 0);
		LineTraderDTO dto = plan("short", null, 5100D, 4800D, State.RUNNING);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(4790));	// 4790 <= 4800 触发止盈

		ArgumentCaptor<SubmitOrderReq> captor = ArgumentCaptor.forClass(SubmitOrderReq.class);
		verify(account).submitOrder(captor.capture());
		assertThat(captor.getValue().direction()).isEqualTo(DirectionEnum.D_Buy);
		assertThat(captor.getValue().offsetFlag()).isEqualTo(OffsetFlagEnum.OF_Close);
		assertThat(dto.getState()).isEqualTo(State.COMPLETED);
	}

	/**
	 * 触发空头止损时自动平仓
	 */
	@Test
	void shouldCloseShortOnStopLoss() {
		givenPosition(PositionDirectionEnum.PD_Short, 2, 0);
		LineTraderDTO dto = plan("short", null, 5100D, 4800D, State.RUNNING);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(5110));	// 5110 >= 5100 触发止损

		ArgumentCaptor<SubmitOrderReq> captor = ArgumentCaptor.forClass(SubmitOrderReq.class);
		verify(account).submitOrder(captor.capture());
		assertThat(captor.getValue().direction()).isEqualTo(DirectionEnum.D_Buy);
		assertThat(captor.getValue().offsetFlag()).isEqualTo(OffsetFlagEnum.OF_Close);
		assertThat(dto.getState()).isEqualTo(State.COMPLETED);
	}

	/**
	 * 平仓数量不超过可用持仓（扣除冻结部分）
	 */
	@Test
	void shouldCapExitVolumeByAvailablePosition() {
		givenPosition(PositionDirectionEnum.PD_Long, 3, 1);
		LineTraderDTO dto = plan("long", null, 4900D, 5100D, State.RUNNING);
		dto.setVolume(10);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(5100));

		ArgumentCaptor<SubmitOrderReq> captor = ArgumentCaptor.forClass(SubmitOrderReq.class);
		verify(account).submitOrder(captor.capture());
		assertThat(captor.getValue().volume()).isEqualTo(2);	// min(10, 3 - 1)
	}

	/**
	 * 无持仓且无挂单时计划自动完成
	 */
	@Test
	void shouldCompleteWhenNoPositionAndNoPendingOrder() {
		LineTraderDTO dto = plan("long", null, 4900D, 5100D, State.RUNNING);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(5000));

		assertThat(dto.getState()).isEqualTo(State.COMPLETED);
		verify(callback).accept(dto);
		verify(account, never()).submitOrder(any());
	}

	/**
	 * 持仓被手动平仓后，计划自动完成（不再监控）
	 */
	@Test
	void shouldCompleteAfterPositionManuallyClosed() {
		givenPosition(PositionDirectionEnum.PD_Long, 2, 0);
		LineTraderDTO dto = plan("long", null, 4900D, 5100D, State.RUNNING);
		LineTrader trader = traderOf(dto);

		// 持有仓位期间，计划保持运行中且不下单
		trader.onTick(tick(5000));
		assertThat(dto.getState()).isEqualTo(State.RUNNING);
		verify(account, never()).submitOrder(any());

		// 模拟持仓被手动平仓，计划自动完成
		when(account.getPosition(eq(PositionDirectionEnum.PD_Long), eq(contract))).thenReturn(Optional.empty());
		trader.onTick(tick(5000));
		assertThat(dto.getState()).isEqualTo(State.COMPLETED);
		verify(callback).accept(dto);
		verify(account, never()).submitOrder(any());
	}

	/**
	 * 入场委托未了结前计划不会误终结，委托成交回报后按计划结束
	 * @throws InterruptedException 
	 */
	@Test
	void shouldNotCompleteUntilEntryOrderDone() throws InterruptedException {
		LineTraderDTO dto = plan("long", 5000D, 4900D, 5200D, State.STANDBY);
		LineTrader trader = traderOf(dto);

		trader.onTick(tick(5010));
		trader.onTick(tick(4990));	// 触发入场，记录挂单
		ArgumentCaptor<SubmitOrderReq> captor = ArgumentCaptor.forClass(SubmitOrderReq.class);
		verify(account).submitOrder(captor.capture());
		String entryOrderId = captor.getValue().originOrderId();

		// 挂单未了结，即使暂无持仓也不应终结计划
		trader.onTick(tick(4990));
		assertThat(dto.getState()).isEqualTo(State.RUNNING);

		// 无关订单回报不影响挂单跟踪
		trader.onOrder(Order.builder().originOrderId(UUID.randomUUID().toString()).orderStatus(OrderStatusEnum.OS_AllTraded).build());
		trader.onTick(tick(4990));
		assertThat(dto.getState()).isEqualTo(State.RUNNING);

		// 入场委托已终结（全部成交），且无持仓时计划完成
		Tick t = tick(4990);
		trader.onOrder(Order.builder().originOrderId(entryOrderId).orderStatus(OrderStatusEnum.OS_AllTraded).contract(t.contract()).build());
		Thread.sleep(1000L);
		trader.onTick(t);
		assertThat(dto.getState()).isEqualTo(State.COMPLETED);
	}

	/**
	 * 未知持仓方向抛出异常
	 */
	@Test
	void shouldThrowOnUnknownDirection() {
		LineTraderDTO dto = plan("unknown", null, 4900D, 5100D, State.RUNNING);
		LineTrader trader = traderOf(dto);

		assertThatThrownBy(trader::hasPosition).isInstanceOf(IllegalArgumentException.class);
	}

	/* ==================== 测试工具 ==================== */

	private void givenPosition(PositionDirectionEnum dir, double position, double frozen) {
		when(account.getPosition(eq(dir), eq(contract)))
				.thenReturn(Optional.of(Position.builder()
						.position(position)
						.frozen(frozen)
						.contract(contract)
						.build()));
	}

	private LineTraderDTO plan(String dir, Double orderPrice, Double stopLoss, Double takeProfit, State state) {
		return LineTraderDTO.builder()
				.id(UUID.randomUUID().toString())
				.positionDir(dir)
				.orderPrice(orderPrice)
				.stopLossPrice(stopLoss)
				.takeProfitPrice(takeProfit)
				.accountGatewayId(GATEWAY_ID)
				.contractId(CONTRACT_ID)
				.contractName("螺纹钢2501")
				.volume(1)
				.state(state)
				.build();
	}

	private LineTrader traderOf(LineTraderDTO dto) {
		return LineTrader.builder()
				.data(dto)
				.accountManager(accMgr)
				.marketCenter(mktCenter)
				.stateChangeCallback(callback)
				.build();
	}

	private Tick tick(double price) {
		return tick(contract, price);
	}

	private Tick tick(Contract c, double price) {
		LocalDateTime now = LocalDateTime.now();
		return Tick.builder()
				.gatewayId("SIM")
				.contract(c)
				.lastPrice(price)
				.actionDay(now.toLocalDate())
				.actionTime(now.toLocalTime())
				.actionTimestamp(System.currentTimeMillis())
				.build();
	}

}
