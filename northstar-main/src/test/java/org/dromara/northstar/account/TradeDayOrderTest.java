package org.dromara.northstar.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.dromara.northstar.account.TradeDayOrder;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;

public class TradeDayOrderTest {
	
	TradeDayOrder tdo = new TradeDayOrder();
	
	ContractField contract = ContractField.newBuilder()
			.setContractId("rb2102@SHFE")
			.setExchange(ExchangeEnum.SHFE)
			.setGatewayId("testGateway")
			.setSymbol("rb2102")
			.setMultiplier(10)
			.setLongMarginRatio(0.08)
			.setShortMarginRatio(0.08)
			.build();
	

	@Test
	public void testUpdate() {
		OrderField order1 = OrderField.newBuilder()
				.setAccountId("testGateway")
				.setOriginOrderId("123")
				.setContract(contract)
				.setOrderStatus(OrderStatusEnum.OS_Unknown)
				.build();
		
		OrderField order2 = OrderField.newBuilder()
				.setAccountId("testGateway")
				.setOriginOrderId("456")
				.setContract(contract)
				.setOrderStatus(OrderStatusEnum.OS_Rejected)
				.build();
		
		tdo.update(order1);
		tdo.update(order2);
		assertThat(tdo.getOrders().size()).isEqualTo(2);
	}

	@Test
	public void testCanCancelOrder() {
		testUpdate();
		assertThat(tdo.canCancelOrder("123")).isTrue();
		assertThat(tdo.canCancelOrder("456")).isFalse();
	}

}
