package org.dromara.northstar.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.TradeAccount;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.NoSuchContractException;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.LineTraderDTO;
import org.dromara.northstar.common.model.LineTraderDTO.State;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.dromara.northstar.trader.LineTrader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;

import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 画线交易计划事件处理器单元测试
 */
class PlannedTradeHandlerTest {

	private static final String CONTRACT_ID = "rb2501@SHFE@FUTURES@SIM";
	private static final String GATEWAY_ID = "testAcc";

	AccountManager accMgr = mock(AccountManager.class);
	MarketCenter mktCenter = mock(MarketCenter.class);
	EventEmitterHolder emitterHolder = mock(EventEmitterHolder.class);
	IContract gatewayContract = mock(IContract.class);
	TradeAccount account = mock(TradeAccount.class);

	PlannedTradeHandler handler;

	Contract contract = Contract.builder()
			.contractId(CONTRACT_ID)
			.name("螺纹钢2501")
			.priceTick(1)
			.build();

	@BeforeAll
	static void initMessageSource() {
		MessageSourceHolder.setMessageSource(mock(MessageSource.class));
	}

	@BeforeEach
	void prepare() {
		handler = new PlannedTradeHandler(accMgr, mktCenter, emitterHolder);
		when(accMgr.get(any())).thenReturn(account);
		when(mktCenter.getContract(any(Identifier.class))).thenReturn(gatewayContract);
		when(gatewayContract.contract()).thenReturn(contract);
	}

	/**
	 * 只处理行情与成交事件
	 */
	@Test
	void shouldOnlyHandleTickAndTrade() {
		assertThat(handler.canHandle(NorthstarEventType.TICK)).isTrue();
		assertThat(handler.canHandle(NorthstarEventType.TRADE)).isTrue();
		assertThat(handler.canHandle(NorthstarEventType.ORDER)).isTrue();
		assertThat(handler.canHandle(NorthstarEventType.ACCOUNT)).isFalse();
		assertThat(handler.canHandle(NorthstarEventType.BAR)).isFalse();
	}

	/**
	 * 多头计划的止盈价低于入场价时拒绝创建
	 */
	@Test
	void shouldRejectLongPlanWithInvalidPrices() {
		LineTraderDTO dto = plan("long", 5000D, 4900D, 4990D);	// 止盈价低于入场价

		assertThatThrownBy(() -> handler.addTrader(dto)).isInstanceOf(OperationException.class);
		assertThat(handler.has(dto.getId())).isFalse();
	}

	/**
	 * 空头计划的止损价低于入场价时拒绝创建
	 */
	@Test
	void shouldRejectShortPlanWithInvalidPrices() {
		LineTraderDTO dto = plan("short", 5000D, 4950D, 4800D);	// 止损价低于入场价

		assertThatThrownBy(() -> handler.addTrader(dto)).isInstanceOf(OperationException.class);
		assertThat(handler.has(dto.getId())).isFalse();
	}

	/**
	 * 运行中的计划允许只修改止盈价与止损价，且状态不会被请求体重置
	 */
	@Test
	void shouldAllowUpdatingExitPricesOfRunningPlan() {
		LineTraderDTO original = plan("long", 5000D, 4900D, 5100D);
		handler.addTrader(original);
		original.setState(State.RUNNING);
		LineTraderDTO updated = updateOf(original, 5000D, 4800D, 5150D);

		handler.addTrader(updated);

		assertThat(updated.getState()).isEqualTo(State.RUNNING);
		assertThat(updated.getOrderPrice()).isEqualTo(5000D);
		assertThat(updated.getStopLossPrice()).isEqualTo(4800D);
		assertThat(updated.getTakeProfitPrice()).isEqualTo(5150D);
		assertThat(handler.getTraders()).containsExactly(updated);
	}

	/**
	 * 运行中的计划不允许修改超过一个最小变动价位的委托价
	 */
	@Test
	void shouldRejectOrderPriceChangeOverOnePriceTickOfRunningPlan() {
		LineTraderDTO original = plan("long", 5000D, 4900D, 5100D);
		handler.addTrader(original);
		original.setState(State.RUNNING);
		LineTraderDTO updated = updateOf(original, 5002D, 4800D, 5150D);

		assertThatThrownBy(() -> handler.addTrader(updated)).isInstanceOf(OperationException.class);
		assertThat(handler.getTraders()).containsExactly(original);
		// 校验失败不污染请求对象
		assertThat(updated.getState()).isEqualTo(State.STANDBY);
		assertThat(updated.getOrderPrice()).isEqualTo(5002D);
	}

	/**
	 * 运行中的计划不允许修改恰好一个最小变动价位的委托价
	 */
	@Test
	void shouldRejectOrderPriceChangeExactlyOnePriceTickOfRunningPlan() {
		LineTraderDTO original = plan("long", 5000D, 4900D, 5100D);
		handler.addTrader(original);
		original.setState(State.RUNNING);
		LineTraderDTO updated = updateOf(original, 5001D, 4800D, 5150D);

		assertThatThrownBy(() -> handler.addTrader(updated)).isInstanceOf(OperationException.class);
		assertThat(handler.getTraders()).containsExactly(original);
	}

	/**
	 * 运行中的计划允许小于一个最小变动价位的委托价误差，但仍保留原委托价
	 */
	@Test
	void shouldKeepOriginalOrderPriceWhenDeviationIsBelowOnePriceTick() {
		LineTraderDTO original = plan("long", 5000D, 4900D, 5100D);
		handler.addTrader(original);
		original.setState(State.RUNNING);
		LineTraderDTO updated = updateOf(original, 5000.4D, 4800D, 5150D);

		handler.addTrader(updated);

		assertThat(updated.getOrderPrice()).isEqualTo(5000D);
		assertThat(handler.getTraders()).containsExactly(updated);
	}

	/**
	 * 运行中的组合计划不允许移除原委托价
	 */
	@Test
	void shouldRejectRemovingOrderPriceOfRunningPlan() {
		LineTraderDTO original = plan("long", 5000D, 4900D, 5100D);
		handler.addTrader(original);
		original.setState(State.RUNNING);
		LineTraderDTO updated = updateOf(original, null, 4800D, 5150D);

		assertThatThrownBy(() -> handler.addTrader(updated)).isInstanceOf(OperationException.class);
		assertThat(handler.getTraders()).containsExactly(original);
	}

	/**
	 * 就绪中的计划仍可修改委托价
	 */
	@Test
	void shouldAllowUpdatingOrderPriceOfStandbyPlan() {
		LineTraderDTO original = plan("long", 5000D, 4900D, 5100D);
		handler.addTrader(original);
		LineTraderDTO updated = updateOf(original, 5002D, 4800D, 5150D);

		handler.addTrader(updated);

		assertThat(updated.getState()).isEqualTo(State.STANDBY);
		assertThat(updated.getOrderPrice()).isEqualTo(5002D);
		assertThat(handler.getTraders()).containsExactly(updated);
	}

	/**
	 * 运行中的纯止盈止损计划不允许补加委托价
	 */
	@Test
	void shouldRejectAddingOrderPriceToRunningExitOnlyPlan() {
		LineTraderDTO original = plan("long", null, 4900D, 5100D);
		handler.addTrader(original);
		original.setState(State.RUNNING);
		LineTraderDTO updated = updateOf(original, 5000D, 4800D, 5150D);

		assertThatThrownBy(() -> handler.addTrader(updated)).isInstanceOf(OperationException.class);
		assertThat(handler.getTraders()).containsExactly(original);
		assertThat(updated.getOrderPrice()).isEqualTo(5000D);
	}

	/**
	 * 运行中的空头计划同样不允许修改超过一个最小变动价位的委托价
	 */
	@Test
	void shouldRejectOrderPriceChangeOfRunningShortPlan() {
		LineTraderDTO original = plan("short", 5000D, 5100D, 4900D);
		handler.addTrader(original);
		original.setState(State.RUNNING);
		LineTraderDTO updated = updateOf(original, 4998D, 5100D, 4900D);

		assertThatThrownBy(() -> handler.addTrader(updated)).isInstanceOf(OperationException.class);
		assertThat(handler.getTraders()).containsExactly(original);
	}

	/**
	 * 运行中的计划允许将止损价调整到入场价上方（移动止损），不受初始建仓校验约束
	 */
	@Test
	void shouldAllowTrailingStopAboveEntryPriceOfRunningPlan() {
		LineTraderDTO original = plan("long", 5000D, 4900D, 5100D);
		handler.addTrader(original);
		original.setState(State.RUNNING);
		LineTraderDTO updated = updateOf(original, 5000D, 5100D, 5200D);

		handler.addTrader(updated);

		assertThat(updated.getState()).isEqualTo(State.RUNNING);
		assertThat(updated.getStopLossPrice()).isEqualTo(5100D);
		assertThat(updated.getTakeProfitPrice()).isEqualTo(5200D);
		assertThat(handler.getTraders()).containsExactly(updated);
	}

	/**
	 * 运行中的计划除止盈止损价外的关键字段被冻结，防止同ID重保存篡改
	 */
	@Test
	void shouldFreezeNonPriceFieldsOfRunningPlan() {
		LineTraderDTO original = plan("long", 5000D, 4900D, 5100D);
		handler.addTrader(original);
		original.setState(State.RUNNING);
		LineTraderDTO updated = updateOf(original, 5000D, 4800D, 5150D);
		updated.setPositionDir("short");
		updated.setContractId("other@SHFE@FUTURES@SIM");
		updated.setAccountGatewayId("otherAcc");
		updated.setVolume(99);

		handler.addTrader(updated);

		assertThat(updated.getPositionDir()).isEqualTo("long");
		assertThat(updated.getContractId()).isEqualTo(CONTRACT_ID);
		assertThat(updated.getAccountGatewayId()).isEqualTo(GATEWAY_ID);
		assertThat(updated.getVolume()).isEqualTo(1);
		assertThat(updated.getStopLossPrice()).isEqualTo(4800D);
		assertThat(updated.getTakeProfitPrice()).isEqualTo(5150D);
		assertThat(handler.getTraders()).containsExactly(updated);
	}

	/**
	 * 运行中的计划更新止盈止损时保留未成交的入场委托状态，不会被误清理
	 */
	@Test
	void shouldPreservePendingEntryOrderWhenUpdatingRunningPlan() {
		LineTraderDTO original = plan("long", 5000D, 4900D, 5100D);
		handler.addTrader(original);
		handler.onEvent(new NorthstarEvent(NorthstarEventType.TICK, tick(5010)));
		handler.onEvent(new NorthstarEvent(NorthstarEventType.TICK, tick(4990)));	// 触发入场，委托未成交
		verify(account).submitOrder(any());
		LineTraderDTO updated = updateOf(original, 5000D, 4800D, 5150D);

		handler.addTrader(updated);
		handler.onEvent(new NorthstarEvent(NorthstarEventType.TICK, tick(5005)));	// 暂无持仓但入场委托仍在途

		assertThat(updated.getState()).isEqualTo(State.RUNNING);
		assertThat(handler.getTraders()).containsExactly(updated);
	}

	/**
	 * 能正常添加并查询交易计划
	 */
	@Test
	void shouldAddAndListTraders() {
		LineTraderDTO dto = plan("long", 4000D, 3900D, 4100D);

		handler.addTrader(dto);

		assertThat(handler.has(dto.getId())).isTrue();
		assertThat(handler.getTraders()).containsExactly(dto);
		verify(emitterHolder).send(any());	// 广播 LINE_TRADERS 列表
	}

	/**
	 * 已持仓且无入场价的计划，添加后自动进入运行中并广播状态变更
	 */
	@Test
	void shouldBecomeRunningWhenPositionExistsAndNoEntryPrice() {
		when(account.getPosition(eq(PositionDirectionEnum.PD_Long), eq(contract)))
				.thenReturn(Optional.of(Position.builder().position(1).frozen(0).contract(contract).build()));
		LineTraderDTO dto = plan("long", null, 4900D, 5100D);

		handler.addTrader(dto);

		assertThat(dto.getState()).isEqualTo(State.RUNNING);
		// 一次 LINE_TRADERS 列表广播 + 一次异步的 LINE_TRADER_RUNNING 状态广播
		verify(emitterHolder, timeout(3000).atLeast(2)).send(any());
	}

	/**
	 * 能手动移除交易计划
	 */
	@Test
	void shouldRemoveTrader() {
		LineTraderDTO dto = plan("long", 4000D, 3900D, 4100D);
		handler.addTrader(dto);

		LineTrader removal = handler.removeTrader(dto.getId());

		assertThat(removal).isNotNull();
		assertThat(removal.getData()).isEqualTo(dto);
		assertThat(handler.has(dto.getId())).isFalse();
		assertThat(handler.getTraders()).isEmpty();
		verify(emitterHolder, times(2)).send(any());	// 添加与移除各广播一次列表
	}

	/**
	 * 行情事件驱动计划触发入场委托
	 */
	@Test
	void shouldTriggerEntryOnTickEvent() {
		LineTraderDTO dto = plan("long", 5000D, 4900D, 5200D);
		handler.addTrader(dto);

		handler.onEvent(new NorthstarEvent(NorthstarEventType.TICK, tick(5010)));
		handler.onEvent(new NorthstarEvent(NorthstarEventType.TICK, tick(4990)));	// 向下穿越入场价

		ArgumentCaptor<SubmitOrderReq> captor = ArgumentCaptor.forClass(SubmitOrderReq.class);
		verify(account).submitOrder(captor.capture());
		assertThat(captor.getValue().offsetFlag()).isEqualTo(OffsetFlagEnum.OF_Open);
		assertThat(dto.getState()).isEqualTo(State.RUNNING);
		assertThat(handler.has(dto.getId())).isTrue();
	}

	/**
	 * 成交事件仅透传给各交易计划，不会导致计划移除
	 */
	@Test
	void shouldDispatchTradeEvent() {
		LineTraderDTO dto = plan("long", 4000D, 3900D, 4100D);
		handler.addTrader(dto);

		handler.onEvent(new NorthstarEvent(NorthstarEventType.TRADE, Trade.builder().contract(contract).build()));

		assertThat(handler.has(dto.getId())).isTrue();
		assertThat(dto.getState()).isEqualTo(State.STANDBY);
	}

	/**
	 * 已完成状态的计划在处理事件后被自动移除
	 */
	@Test
	void shouldRemoveCompletedTraderAfterEvent() {
		LineTraderDTO dto = plan("long", 4000D, 3900D, 4100D);
		handler.addTrader(dto);
		dto.setState(State.COMPLETED);

		handler.onEvent(new NorthstarEvent(NorthstarEventType.TICK, tick(5000)));

		assertThat(handler.has(dto.getId())).isFalse();
		assertThat(handler.getTraders()).isEmpty();
	}

	/**
	 * 不处理未订阅的事件类型
	 */
	@Test
	void shouldIgnoreUnhandledEventType() {
		LineTraderDTO dto = plan("long", 4000D, 3900D, 4100D);
		handler.addTrader(dto);

		handler.onEvent(new NorthstarEvent(NorthstarEventType.ORDER, new Object()));

		verify(account, never()).submitOrder(any());
		assertThat(handler.has(dto.getId())).isTrue();
	}

	/**
	 * 行情处理异常时向前端广播警告，且计划不被移除
	 */
	@Test
	void shouldNotifyWarningWhenTickHandlingFails() {
		LineTraderDTO dto = plan("long", null, 4900D, 5100D);	// 无入场价，行情处理依赖持仓查询
		handler.addTrader(dto);
		// 添加后模拟合约查询故障，使持仓检查抛异常（异常需提前构造，避免在 stubbing 中调用 MessageSource mock）
		NoSuchContractException ex = new NoSuchContractException(CONTRACT_ID);
		when(mktCenter.getContract(any(Identifier.class))).thenThrow(ex);

		handler.onEvent(new NorthstarEvent(NorthstarEventType.TICK, tick(5000)));

		verify(emitterHolder, times(2)).send(any());	// LINE_TRADERS 列表广播 + COMS_WARN 警告广播
		assertThat(handler.has(dto.getId())).isTrue();
	}

	/* ==================== 测试工具 ==================== */

	private LineTraderDTO plan(String dir, Double orderPrice, Double stopLoss, Double takeProfit) {
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
				.state(State.STANDBY)
				.build();
	}

	private LineTraderDTO updateOf(LineTraderDTO original, Double orderPrice, Double stopLoss, Double takeProfit) {
		LineTraderDTO updated = plan(original.getPositionDir(), orderPrice, stopLoss, takeProfit);
		updated.setId(original.getId());
		return updated;
	}

	private Tick tick(double price) {
		LocalDateTime now = LocalDateTime.now();
		return Tick.builder()
				.gatewayId("SIM")
				.contract(contract)
				.lastPrice(price)
				.actionDay(now.toLocalDate())
				.actionTime(now.toLocalTime())
				.actionTimestamp(System.currentTimeMillis())
				.build();
	}

}
