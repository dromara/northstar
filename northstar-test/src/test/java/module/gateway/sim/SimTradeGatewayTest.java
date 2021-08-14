package module.gateway.sim;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Before;
import org.junit.Test;

import common.TestFieldUtil;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.common.utils.CommonUtils;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.sim.trade.SimFactory;
import tech.xuanwu.northstar.gateway.sim.trade.SimTradeGateway;
import tech.xuanwu.northstar.gateway.sim.trade.SimTradeGatewayLocal;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
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
public class SimTradeGatewayTest {
	
	private SimTradeGateway gateway;
	
	private FastEventEngine feEngine = mock(FastEventEngine.class);
	
	private static final String RB = "rb2201";
	
	@Before
	public void prepare() {
		GatewaySettingField setting = TestFieldUtil.makeGatewaySetting();
		ContractField contract = TestFieldUtil.makeContract(RB);
		ContractManager contractMgr = mock(ContractManager.class);
		when(contractMgr.getContract(contract.getUnifiedSymbol())).thenReturn(contract);
		SimFactory factory = new SimFactory(setting.getGatewayId(), feEngine, 1, contractMgr);
		gateway = new SimTradeGatewayLocal(feEngine, setting, factory.newGwAccountHolder());
	}
	
	/**
	 * 入金验证
	 */
	@Test
	public void testMoneyIn() {
		gateway.moneyIO(20000);
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 20000));
	}

	/**
	 * 出金验证
	 * @throws InterruptedException 
	 */
	@Test
	public void testMoneyOut() throws InterruptedException {
		gateway.moneyIO(20000);
		Thread.sleep(1200);
		gateway.moneyIO(-100);
		verify(feEngine,times(2)).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 19900));
	}
	
	/**
	 * 出金异常验证
	 */
	@Test(expected = IllegalStateException.class)
	public void testMoneyOutException() throws InterruptedException {
		gateway.moneyIO(-100);
	}
	
	/**
	 * 无持仓状态下行情更新验证
	 * @throws InterruptedException 
	 */
	@Test
	public void testTickUpdate() throws InterruptedException {
		for(int i=0; i<3; i++) {
			int price = ThreadLocalRandom.current().nextInt(2000, 20000);
			gateway.onTick(TestFieldUtil.makeTickField(RB, price));
			verify(feEngine, times(i + 1)).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getBalance() == 0));
			Thread.sleep(1200);
		}
	}
	
	/**
	 * 有持仓状态下行情更新验证
	 */
	@Test
	public void testTickUpdateOnHolding() {
		testOpeningTrade();
		gateway.onTick(TestFieldUtil.makeTickField(RB, 1230));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPriceDiff() == 29));
		gateway.onTick(TestFieldUtil.makeTickField(RB, 1150));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPriceDiff() == -51));
	}
	
	/**
	 * 无持仓条件下委托测试
	 */
	@Test
	public void testOpeningSubmitOrder() {
		gateway.moneyIO(10000);
		gateway.submitOrder(TestFieldUtil.makeOrderReq(RB, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1234, 1000));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_Open
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Touched));
	}
	
	/**
	 * 无持仓条件下资金不足委托测试
	 */
	@Test
	public void testOpeningSubmitOrderFail() {
		gateway.submitOrder(TestFieldUtil.makeOrderReq(RB, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1234, 0));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_Open 
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Rejected));
	}
	
	/**
	 * 持仓条件下委托测试
	 */
	@Test
	public void testClosingSubmitOrder() {
		testOpeningTrade();
		
		SubmitOrderReqField orderReq = TestFieldUtil.makeOrderReq(RB, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 1, 1234, 1000);
		gateway.submitOrder(orderReq);
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_CloseToday
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Touched));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 1
				&& ((PositionField)pos).getFrozen() == 1 && ((PositionField)pos).getTdFrozen() == 1));
	}
	
	/**
	 * 持仓条件下持仓不足委托测试
	 */
	@Test
	public void testClosingSubmitOrderFail() {
		testOpeningTrade();
		
		SubmitOrderReqField orderReq = TestFieldUtil.makeOrderReq(RB, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 2, 1234, 1000);
		gateway.submitOrder(orderReq);
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_CloseToday
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Rejected));
	}
	
	/**
	 * 加仓测试
	 */
	@Test
	public void testIncreasingPosition() {
		testOpeningTrade();
		
		SubmitOrderReqField orderReq = TestFieldUtil.makeOrderReq(RB, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 2, 1234, 1000);
		gateway.submitOrder(orderReq);
		gateway.onTick(TestFieldUtil.makeTickField(RB, 1111));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1112));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 3
				&& CommonUtils.isEquals(((PositionField)pos).getOpenPrice(), 1141.66666666)));
		
	}
	
	/**
	 * 减仓测试
	 */
	@Test
	public void testDecreasingPosition() {
		testIncreasingPosition();
		
		SubmitOrderReqField orderReq = TestFieldUtil.makeOrderReq(RB, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 1, 1234, 1000);
		gateway.submitOrder(orderReq);
		gateway.onTick(TestFieldUtil.makeTickField(RB, 1235));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1234));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 2
				&& CommonUtils.isEquals(((PositionField)pos).getOpenPrice(), 1141.66666666)));
	}
	
	/**
	 * 止损测试
	 * @throws InterruptedException 
	 */
	@Test
	public void testStopLoss() throws InterruptedException {
		testOpeningTrade();
		
		gateway.onTick(TestFieldUtil.makeTickField(RB, 1000));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1000));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 0));
		Thread.sleep(1200);
		gateway.onTick(TestFieldUtil.makeTickField(RB, 1000));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getMargin() == 0
				&& ((AccountField)acc).getCloseProfit() == -2010 && ((AccountField)acc).getCommission() == 20));
	}
	
	/**
	 * 无持仓状态下撤单测试
	 */
	@Test
	public void testCancelOrder() {
		gateway.moneyIO(10000);
		SubmitOrderReqField orderReq = TestFieldUtil.makeOrderReq(RB, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1234, 1000);
		CancelOrderReqField cancelReq = TestFieldUtil.makeCancelReq(orderReq);
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
	public void testCancelOrderOnHolding() {
		testOpeningTrade();
		
		SubmitOrderReqField orderReq = TestFieldUtil.makeOrderReq(RB, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 1, 1234, 1000);
		CancelOrderReqField cancelReq = TestFieldUtil.makeCancelReq(orderReq);
		gateway.submitOrder(orderReq);
		verify(feEngine).emitEvent(eq(NorthstarEventType.ORDER), argThat(order -> ((OrderField)order).getOffsetFlag() == OffsetFlagEnum.OF_CloseToday
				&& ((OrderField)order).getOrderStatus() == OrderStatusEnum.OS_Touched));
		gateway.cancelOrder(cancelReq);
		verify(feEngine, times(2)).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 1
				&& ((PositionField)pos).getFrozen() == 0 && ((PositionField)pos).getTdFrozen() == 0));
	}
	
	/**
	 * 开仓委托成交测试
	 */
	@Test
	public void testOpeningTrade() {
		testOpeningSubmitOrder();
		
		gateway.onTick(TestFieldUtil.makeTickField(RB, 1200));
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
	public void testClosingTrade() throws InterruptedException {
		testOpeningTrade();
		
		SubmitOrderReqField orderReq = TestFieldUtil.makeOrderReq(RB, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 1, 1234, 1000);
		gateway.submitOrder(orderReq);
		gateway.onTick(TestFieldUtil.makeTickField(RB, 1235));
		verify(feEngine).emitEvent(eq(NorthstarEventType.TRADE), argThat(trade -> ((TradeField)trade).getPrice() == 1234));
		verify(feEngine).emitEvent(eq(NorthstarEventType.POSITION), argThat(pos -> ((PositionField)pos).getPosition() == 0));
		Thread.sleep(1200);
		gateway.onTick(TestFieldUtil.makeTickField(RB, 1235));
		verify(feEngine).emitEvent(eq(NorthstarEventType.ACCOUNT), argThat(acc -> ((AccountField)acc).getMargin() == 0
				&& ((AccountField)acc).getCloseProfit() == 330 && ((AccountField)acc).getCommission() == 20));
	}
}
