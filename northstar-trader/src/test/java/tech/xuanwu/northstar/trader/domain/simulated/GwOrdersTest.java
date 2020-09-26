package tech.xuanwu.northstar.trader.domain.simulated;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Arrays;
import org.junit.Before;
import org.junit.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class GwOrdersTest {

	GwAccount account;
	
	GwPositions gwPositions;
	GwOrders gwOrders;
	
	SubmitOrderReqField submitOrderReq;
	CancelOrderReqField cancelOrderReq;
	
	TickField tick;
	ContractField contract;
	
	@Before
	public void prepare() {
		contract = ContractField.newBuilder()
				.setUnifiedSymbol("rb2010@Test")
				.setSymbol("rb2010")
				.setLongMarginRatio(0.1)
				.setShortMarginRatio(0.1)
				.setMultiplier(10)
				.build();
		Map<String, ContractField> contractMap = new HashMap<>();
		contractMap.put("rb2010@Test", contract);
		
		AccountField af = AccountField.newBuilder()
				.setAccountId("test")
				.setGatewayId("test")
				.setDeposit(10000)
				.build();
		account = new GwAccount(af);
		
		gwPositions = new GwPositions();
		gwPositions.setGwAccount(account);
		gwPositions.setContractMap(contractMap);
		
		gwOrders = new GwOrders();
		gwOrders.setGwAccount(account);
		gwOrders.setGwPositions(gwPositions);
		gwOrders.setContractMap(contractMap);
		
		account.setGwOrders(gwOrders);
		account.setGwPositions(gwPositions);
		
		gwPositions.addPosition(TradeField.newBuilder()
				.setContract(contract)
				.setVolume(1)
				.setPrice(3000)
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.build());
		
		submitOrderReq = SubmitOrderReqField.newBuilder()
				.build();
		
		cancelOrderReq = CancelOrderReqField.newBuilder()
				.build();
		
		tick = TickField.newBuilder()
				.addAllAskPrice(List.of(2999D))
				.setUnifiedSymbol("rb2010@Test")
				.build();
	}
	
	@Test
	public void testSubmitOrderSuccess() {
		submitOrderReq = submitOrderReq.toBuilder()
				.setContract(contract)
				.setPrice(3000)
				.setVolume(1)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setDirection(DirectionEnum.D_Buy)
				.build();
		OrderField order = gwOrders.submitOrder(submitOrderReq);
		assertThat(order.getOrderStatus()).isEqualTo(OrderStatusEnum.OS_Touched);
		
		submitOrderReq = submitOrderReq.toBuilder()
				.setContract(contract)
				.setPrice(3000)
				.setVolume(1)
				.setOffsetFlag(OffsetFlagEnum.OF_Close)
				.setDirection(DirectionEnum.D_Sell)
				.build();
		OrderField order2 = gwOrders.submitOrder(submitOrderReq);
		assertThat(order2.getOrderStatus()).isEqualTo(OrderStatusEnum.OS_Touched);
	}
	
	@Test
	public void testSubmitOrderFail() {
		submitOrderReq = submitOrderReq.toBuilder()
				.setContract(contract)
				.setPrice(3000)
				.setVolume(10)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setDirection(DirectionEnum.D_Buy)
				.build();
		OrderField order = gwOrders.submitOrder(submitOrderReq);
		assertThat(order.getOrderStatus()).isEqualTo(OrderStatusEnum.OS_Rejected);
		
		submitOrderReq = submitOrderReq.toBuilder()
				.setContract(contract)
				.setPrice(3000)
				.setVolume(2)
				.setOffsetFlag(OffsetFlagEnum.OF_Close)
				.setDirection(DirectionEnum.D_Sell)
				.build();
		OrderField order2 = gwOrders.submitOrder(submitOrderReq);
		assertThat(order2.getOrderStatus()).isEqualTo(OrderStatusEnum.OS_Rejected);
	}
	
	@Test
	public void testCancelOrderSuccess() {
		submitOrderReq = submitOrderReq.toBuilder()
				.setContract(contract)
				.setPrice(3000)
				.setVolume(1)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setDirection(DirectionEnum.D_Buy)
				.build();
		OrderField order = gwOrders.submitOrder(submitOrderReq);
		
		cancelOrderReq = cancelOrderReq.toBuilder()
				.setOrderId(order.getOrderId())
				.build();
		OrderField order2 = gwOrders.cancelOrder(cancelOrderReq);
		assertThat(order2.getOrderStatus()).isEqualTo(OrderStatusEnum.OS_Canceled);
	}
	
	@Test
	public void testCancelOrderFail() {
		cancelOrderReq = cancelOrderReq.toBuilder()
				.setOrderId("123456")
				.build();
		OrderField order = gwOrders.cancelOrder(cancelOrderReq);
		assertThat(order).isNull();
	}
	
	@Test
	public void testDeal() {
		submitOrderReq = submitOrderReq.toBuilder()
				.setContract(contract)
				.setPrice(3000)
				.setVolume(1)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setDirection(DirectionEnum.D_Buy)
				.build();
		OrderField order = gwOrders.submitOrder(submitOrderReq);
		
		List<TradeField> tradeList = gwOrders.tryDeal(tick);
		assertThat(tradeList.size()).isEqualTo(1);
		
	}
}
