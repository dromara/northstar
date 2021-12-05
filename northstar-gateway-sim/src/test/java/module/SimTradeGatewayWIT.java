package module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.ContractManager;
import tech.quantit.northstar.common.utils.CommonUtils;
import tech.quantit.northstar.gateway.sim.trade.SimFactory;
import tech.quantit.northstar.gateway.sim.trade.SimTradeGateway;
import tech.quantit.northstar.gateway.sim.trade.SimTradeGatewayLocal;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模拟网关账户测试
 * @author KevinHuangwl
 *
 */
class SimTradeGatewayWIT {
	
	private SimTradeGateway gateway;
	
	private FastEventEngine feEngine = mock(FastEventEngine.class);
	
	private static final String RB = "rb2201";
	
	private TestFieldFactory ff = new TestFieldFactory("testGateway");
	
	@BeforeEach
	void prepare() {
		GatewaySettingField setting = ff.makeGatewaySetting();
		ContractField contract = ff.makeContract(RB);
		ContractManager contractMgr = mock(ContractManager.class);
		when(contractMgr.getContract(contract.getUnifiedSymbol())).thenReturn(contract);
		SimFactory factory = new SimFactory(setting.getGatewayId(), feEngine, 1, contractMgr);
		gateway = new SimTradeGatewayLocal(feEngine, setting, factory.newGwAccountHolder());
	}
	
	/**
	 * 入金验证
	 */
	@Test
	void testMoneyIn() {
		assertThat(gateway.moneyIO(20000)).isEqualTo(20000);
	}

	/**
	 * 出金验证
	 * @throws InterruptedException 
	 */
	@Test
	void testMoneyOut() throws InterruptedException {
		assertThat(gateway.moneyIO(20000)).isEqualTo(20000);
		assertThat(gateway.moneyIO(-1000)).isEqualTo(19000);
	}
	
	/**
	 * 出金异常验证
	 */
	@Test
	void testMoneyOutException() throws InterruptedException {
		assertThrows(IllegalStateException.class, ()->{			
			gateway.moneyIO(-100);
		});
	}
	
	/**
	 * 无持仓状态下行情更新验证
	 * @throws InterruptedException 
	 */
	@Test
	void testTickUpdate() throws InterruptedException {
		for(int i=0; i<3; i++) {
			int price = ThreadLocalRandom.current().nextInt(2000, 20000);
			gateway.onTick(ff.makeTickField(RB, price));
			verify(feEngine, times(i + 1)).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 0));
			Thread.sleep(1200);
		}
	}
	
	/**
	 * 有持仓状态下行情更新验证
	 */
	@Test
	void testTickUpdateOnHolding() {
		testOpeningTrade();
		gateway.onTick(ff.makeTickField(RB, 1230));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPriceDiff() == 29));
		gateway.onTick(ff.makeTickField(RB, 1150));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPriceDiff() == -51));
	}
	
	/**
	 * 无持仓条件下委托测试
	 */
	@Test
	void testOpeningSubmitOrder() {
		gateway.moneyIO(10000);
		gateway.submitOrder(ff.makeOrderReq(RB, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1234, 1000));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_Open
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Touched));
	}
	
	/**
	 * 无持仓条件下资金不足委托测试
	 */
	@Test
	void testOpeningSubmitOrderFail() {
		gateway.submitOrder(ff.makeOrderReq(RB, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1234, 0));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_Open 
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Rejected));
	}
	
	/**
	 * 持仓条件下委托测试
	 */
	@Test
	void testClosingSubmitOrder() {
		testOpeningTrade();
		
		SubmitOrderReqField orderReq = ff.makeOrderReq(RB, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 1, 1234, 1000);
		gateway.submitOrder(orderReq);
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Touched));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 1
				&& ((PositionField)pos).getFrozen() == 1 && ((PositionField)pos).getTdFrozen() == 1));
	}
	
	/**
	 * 持仓条件下持仓不足委托测试
	 */
	@Test
	void testClosingSubmitOrderFail() {
		testOpeningTrade();
		
		SubmitOrderReqField orderReq = ff.makeOrderReq(RB, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 2, 1234, 1000);
		gateway.submitOrder(orderReq);
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_CloseToday
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Rejected));
	}
	
	/**
	 * 加仓测试
	 */
	@Test
	void testIncreasingPosition() {
		testOpeningTrade();
		
		SubmitOrderReqField orderReq = ff.makeOrderReq(RB, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 2, 1234, 1000);
		gateway.submitOrder(orderReq);
		gateway.onTick(ff.makeTickField(RB, 1111));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1112));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 3
				&& CommonUtils.isEquals(((PositionField)pos).getOpenPrice(), 1141.66666666)));
		
	}
	
	/**
	 * 减仓测试
	 */
	@Test
	void testDecreasingPosition() {
		testIncreasingPosition();
		
		SubmitOrderReqField orderReq = ff.makeOrderReq(RB, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 1, 1234, 1000);
		gateway.submitOrder(orderReq);
		gateway.onTick(ff.makeTickField(RB, 1235));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1234));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 2
				&& CommonUtils.isEquals(((PositionField)pos).getOpenPrice(), 1141.66666666)));
	}
	
	/**
	 * 无持仓状态下撤单测试
	 */
	@Test
	void testCancelOrder() {
		gateway.moneyIO(10000);
		SubmitOrderReqField orderReq = ff.makeOrderReq(RB, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1234, 1000);
		CancelOrderReqField cancelReq = ff.makeCancelReq(orderReq);
		gateway.submitOrder(orderReq);
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_Open
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Touched));
		gateway.cancelOrder(cancelReq);
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_Open
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Canceled));
	}
	
	/**
	 * 持仓状态下撤单测试
	 */
	@Test
	void testCancelOrderOnHolding() {
		testOpeningTrade();
		
		SubmitOrderReqField orderReq = ff.makeOrderReq(RB, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 1, 1234, 1000);
		CancelOrderReqField cancelReq = ff.makeCancelReq(orderReq);
		gateway.submitOrder(orderReq);
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Touched));
		gateway.cancelOrder(cancelReq);
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 1
				&& ((PositionField)pos).getFrozen() == 0 && ((PositionField)pos).getTdFrozen() == 0));
	}
	
	/**
	 * 止损测试
	 */
	@Test
	void testStopLoss() {
		gateway.moneyIO(10000);
		SubmitOrderReqField req = SubmitOrderReqField.newBuilder()
			.setOriginOrderId(UUID.randomUUID().toString())
			.setContract(ff.makeContract(RB))
			.setDirection(DirectionEnum.D_Sell)
			.setOffsetFlag(OffsetFlagEnum.OF_Open)
			.setOrderPriceType(OrderPriceTypeEnum.OPT_AnyPrice)
			.setVolume(1)
			.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
			.setTimeCondition(TimeConditionEnum.TC_GFD)
			.setVolumeCondition(VolumeConditionEnum.VC_AV)
			.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
			.setContingentCondition(ContingentConditionEnum.CC_Immediately)
			.setMinVolume(1)
			.setGatewayId("testGateway")
			.build();
		gateway.submitOrder(req);
		gateway.onTick(ff.makeTickField(RB, 1200));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_AllTraded));
	}
	
	/**
	 * 开仓委托成交测试
	 */
	@Test
	void testOpeningTrade() {
		testOpeningSubmitOrder();
		
		gateway.onTick(ff.makeTickField(RB, 1200));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_Open 
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_AllTraded));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1201));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPriceDiff() == -1));
	}
	
	/**
	 * 平仓委托成交测试 
	 * @throws InterruptedException 
	 */
	@Test
	void testClosingTrade() throws InterruptedException {
		testOpeningTrade();
		
		SubmitOrderReqField orderReq = ff.makeOrderReq(RB, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 1, 1234, 1000);
		gateway.submitOrder(orderReq);
		gateway.onTick(ff.makeTickField(RB, 1235));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1234));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 0));
		Thread.sleep(1200);
		gateway.onTick(ff.makeTickField(RB, 1235));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getMargin() == 0
				&& ((AccountField)acc).getCloseProfit() == 330 && ((AccountField)acc).getCommission() == 20));
	}
}
