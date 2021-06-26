package tech.xuanwu.northstar.integrated.gateway.sim;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.sim.SimFactory;
import tech.xuanwu.northstar.gateway.sim.SimGateway;
import tech.xuanwu.northstar.gateway.sim.SimGatewayLocalImpl;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class SimGatewayTest {
	
	private SimGateway gateway;
	private FastEventEngine feEngine = mock(FastEventEngine.class);
	private ContractField contract = ContractField.newBuilder()
				.setUnifiedSymbol("rb2110@SHFE@FUTURES")
				.setSymbol("rb2110")
				.setLongMarginRatio(0.08)
				.setShortMarginRatio(0.08)
				.setPriceTick(10)
				.setMultiplier(10)
				.build();
	private SubmitOrderReqField submitOrderReq = SubmitOrderReqField.newBuilder()
				.setOriginOrderId("testorder1234")
				.setContract(contract)
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolume(2)
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(TimeConditionEnum.TC_GFD)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setMinVolume(1)
				.setGatewayId("testGatewayId")
				.setPrice(1234)
				.build();
	
	private SubmitOrderReqField submitOrderReq2 = SubmitOrderReqField.newBuilder()
			.setOriginOrderId("testorder1234")
			.setContract(contract)
			.setDirection(DirectionEnum.D_Sell)
			.setOffsetFlag(OffsetFlagEnum.OF_Close)
			.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
			.setVolume(2)
			.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
			.setTimeCondition(TimeConditionEnum.TC_GFD)
			.setVolumeCondition(VolumeConditionEnum.VC_AV)
			.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
			.setContingentCondition(ContingentConditionEnum.CC_Immediately)
			.setMinVolume(1)
			.setGatewayId("testGatewayId")
			.setPrice(1234)
			.build();
	
	@Before
	public void prepare() {
		GatewaySettingField gwSettings = GatewaySettingField.newBuilder()
				.setGatewayId("testGateway")
				.setGatewayType(GatewayTypeEnum.GTE_Trade)
				.build();
		Map<String, ContractField> contractMap = mock(Map.class);
		when(contractMap.get("rb2110@SHFE@FUTURES")).thenReturn(contract);
		
		int costOfCommission = 1;
		SimFactory simFactory = new SimFactory("testGateway", feEngine, costOfCommission, contractMap);
		gateway = new SimGatewayLocalImpl(feEngine, gwSettings, simFactory.newGwAccountHolder());
	}

	// 出入金验证
	@Test
	public void testMoneyIO() {
		gateway.moneyIO(20000);
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 20000));
		
		gateway.moneyIO(-100);
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 19900));
	}

	// 行情更新验证
	@Test
	public void testOnTickWhileEmpty() {
		for(int i=1; i<=10; i++) {
			gateway.onTick(TickField.newBuilder()
					.setUnifiedSymbol("rb2110@SHFE@FUTURES")
					.addAllAskPrice(Arrays.asList(1025D))
					.setLastPrice(1024)
					.addAllBidPrice(Arrays.asList(1023D))
					.build());
			verify(feEngine, times(i)).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> acc instanceof AccountField));
		}
	}
	
	// 持仓更新验证
	@Test
	public void testOnTickWhileHolding() {
		gateway.moneyIO(20000);
		gateway.submitOrder(submitOrderReq);
		gateway.onTick(TickField.newBuilder()
				.setUnifiedSymbol("rb2110@SHFE@FUTURES")
				.addAllAskPrice(Arrays.asList(1025D))
				.setLastPrice(1024)
				.addAllBidPrice(Arrays.asList(1023D))
				.build());
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 19960 && ((AccountField)acc).getCommission() == 20 && ((AccountField)acc).getPositionProfit() == -20));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPrice() == 1025 && ((PositionField)pos).getPosition() == 2 && ((PositionField)pos).getPositionDirection() == PositionDirectionEnum.PD_Long));
		
		gateway.onTick(TickField.newBuilder()
				.setUnifiedSymbol("rb2110@SHFE@FUTURES")
				.addAllAskPrice(Arrays.asList(1125D))
				.setLastPrice(1124)
				.addAllBidPrice(Arrays.asList(1123D))
				.build());
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 21960 && ((AccountField)acc).getCommission() == 20 && ((AccountField)acc).getPositionProfit() == 1980));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPositionProfit() == 1980 && ((PositionField)pos).getPositionDirection() == PositionDirectionEnum.PD_Long));
		
	}

	//没有足够资金开仓
	@Test
	public void testSubmitWithInsufficientAmount() {
		gateway.submitOrder(submitOrderReq);
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getAvailable() == 0));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Rejected));
	}
	
	// 委托与撤单
	@Test
	public void testSubmitAndCancelOrder() {
		gateway.moneyIO(20000);
		gateway.submitOrder(submitOrderReq);
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> acc instanceof AccountField));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_Open));
		
		CancelOrderReqField cancelOrderReq = CancelOrderReqField.newBuilder()
				.setGatewayId("testGatewayId")
				.setOriginOrderId("testorder1234")
				.build();
		gateway.cancelOrder(cancelOrderReq);
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Canceled));
		verify(feEngine, times(3)).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> acc instanceof AccountField));
	}
	
	// 持仓状态下的委托与撤单
	@Test
	public void testSubmitAndCancelOrderOnHolding() {
		//开仓
		testSubmitOrderAndDealOnEmpty();
		//平仓请求
		gateway.submitOrder(submitOrderReq2);
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Touched));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> (((PositionField)pos).getPosition() == 2 && ((PositionField)pos).getFrozen() == 2 && ((PositionField)pos).getTdFrozen() == 2 && ((PositionField)pos).getPositionDirection() == PositionDirectionEnum.PD_Long)));
		
		//撤单
		gateway.cancelOrder(CancelOrderReqField.newBuilder()
				.setOriginOrderId(submitOrderReq.getOriginOrderId())
				.build());
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Canceled));
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> (((PositionField)pos).getPosition() == 2 && ((PositionField)pos).getFrozen() == 0 && ((PositionField)pos).getTdFrozen() == 0 && ((PositionField)pos).getPositionDirection() == PositionDirectionEnum.PD_Long)));
	}

	// 开仓委托与成交
	@Test
	public void testSubmitOrderAndDealOnEmpty() {
		gateway.moneyIO(20000);
		gateway.submitOrder(submitOrderReq);
		gateway.onTick(TickField.newBuilder()
				.setUnifiedSymbol("rb2110@SHFE@FUTURES")
				.addAllAskPrice(Arrays.asList(1025D))
				.setLastPrice(1024)
				.addAllBidPrice(Arrays.asList(1023D))
				.build());
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_AllTraded));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1025));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 19960 && ((AccountField)acc).getCommission() == 20 && ((AccountField)acc).getPositionProfit() == -20));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPrice() == 1025 && ((PositionField)pos).getPosition() == 2 && ((PositionField)pos).getPositionDirection() == PositionDirectionEnum.PD_Long));
	}

	// 平仓委托与成交
	@Test
	public void testSubmitOrderAndDealOnHolding() {
		//开仓
		testSubmitOrderAndDealOnEmpty();
		//平仓请求
		gateway.submitOrder(submitOrderReq2);
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Touched));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> (((PositionField)pos).getPosition() == 2 && ((PositionField)pos).getFrozen() == 2 && ((PositionField)pos).getTdFrozen() == 2 && ((PositionField)pos).getPositionDirection() == PositionDirectionEnum.PD_Long)));
		//成交触发
		gateway.onTick(TickField.newBuilder()
				.setUnifiedSymbol("rb2110@SHFE@FUTURES")
				.addAllAskPrice(Arrays.asList(1236D))
				.setLastPrice(1235)
				.addAllBidPrice(Arrays.asList(1234D))
				.build());
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_AllTraded));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1234));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 24140 && ((AccountField)acc).getCommission() == 40 && ((AccountField)acc).getPositionProfit() == 0 && ((AccountField)acc).getCloseProfit() == 4180));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 0 && ((PositionField)pos).getFrozen() == 0 && ((PositionField)pos).getTdFrozen() == 0 && ((PositionField)pos).getYdFrozen() == 0 && ((PositionField)pos).getPositionProfit() == 0));
	}
	
	// 没有足够仓位平仓
	@Test
	public void testSubmitOrderWithInsufficientVolOnHolding() {
		//开仓
		testSubmitOrderAndDealOnEmpty();
		
		gateway.submitOrder(SubmitOrderReqField.newBuilder()
			.setOriginOrderId("testorder1234")
			.setContract(contract)
			.setDirection(DirectionEnum.D_Sell)
			.setOffsetFlag(OffsetFlagEnum.OF_Close)
			.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
			.setVolume(3)
			.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
			.setTimeCondition(TimeConditionEnum.TC_GFD)
			.setVolumeCondition(VolumeConditionEnum.VC_AV)
			.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
			.setContingentCondition(ContingentConditionEnum.CC_Immediately)
			.setMinVolume(1)
			.setGatewayId("testGatewayId")
			.setPrice(1234)
			.build());
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Rejected));
	}
	
	// 加仓验证
	@Test
	public void testIncreasePosition() {
		//开仓
		testSubmitOrderAndDealOnEmpty();
		//加仓
		gateway.submitOrder(SubmitOrderReqField.newBuilder()
				.setOriginOrderId("testorder1234")
				.setContract(contract)
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolume(3)
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(TimeConditionEnum.TC_GFD)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setMinVolume(1)
				.setGatewayId("testGatewayId")
				.setPrice(1337)
				.build());
		//成交
		gateway.onTick(TickField.newBuilder()
				.setUnifiedSymbol("rb2110@SHFE@FUTURES")
				.addAllAskPrice(Arrays.asList(1337D))
				.setLastPrice(1337)
				.addAllBidPrice(Arrays.asList(1336D))
				.build());
		
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_AllTraded));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1337));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 26190 && ((AccountField)acc).getCommission() == 50 && Math.abs(((AccountField)acc).getPositionProfit() - 6240) < 1 && ((AccountField)acc).getCloseProfit() == 0));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 5 && Math.abs(((PositionField)pos).getPrice() - 1212) < 1 && Math.abs(((PositionField)pos).getPositionProfit() - 6240) < 1));
	}
	
	// 减仓验证
	@Test
	public void testReducePosition() {
		//开仓
		testSubmitOrderAndDealOnEmpty();
		//减仓
		gateway.submitOrder(SubmitOrderReqField.newBuilder()
				.setOriginOrderId("testorder1234")
				.setContract(contract)
				.setDirection(DirectionEnum.D_Sell)
				.setOffsetFlag(OffsetFlagEnum.OF_Close)
				.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolume(1)
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(TimeConditionEnum.TC_GFD)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setMinVolume(1)
				.setGatewayId("testGatewayId")
				.setPrice(1234)
				.build());
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Touched));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> (((PositionField)pos).getPosition() == 2 && ((PositionField)pos).getFrozen() == 1 && ((PositionField)pos).getTdFrozen() == 1 && ((PositionField)pos).getPositionDirection() == PositionDirectionEnum.PD_Long)));
		//成交触发
		gateway.onTick(TickField.newBuilder()
				.setUnifiedSymbol("rb2110@SHFE@FUTURES")
				.addAllAskPrice(Arrays.asList(1235D))
				.setLastPrice(1234)
				.addAllBidPrice(Arrays.asList(1234D))
				.build());
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_AllTraded));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1234));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 24150 && ((AccountField)acc).getCommission() == 30 && ((AccountField)acc).getPositionProfit() == 2090 && ((AccountField)acc).getCloseProfit() == 2090));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 1 && ((PositionField)pos).getFrozen() == 0 && ((PositionField)pos).getTdFrozen() == 0 && ((PositionField)pos).getYdFrozen() == 0));
	}
}
