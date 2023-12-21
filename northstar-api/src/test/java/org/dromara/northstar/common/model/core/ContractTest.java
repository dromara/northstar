package org.dromara.northstar.common.model.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.junit.jupiter.api.Test;
import xyz.redtorch.pb.CoreEnum;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

class ContractTest {

	@Test
	void testToContract() {
		Contract contract = Contract.builder()
				.gatewayId("gatewayId")
				.contractId("contractId")
				.unifiedSymbol("unifiedSymbol")
				.name("name")
				.fullName("fullName")
				.currency(CoreEnum.CurrencyEnum.CAD)
				.productClass(CoreEnum.ProductClassEnum.FUTURES)
				.multiplier(1.0)
				.priceTick(2.0)
				.longMarginRatio(3.0)
				.shortMarginRatio(4.0)
				.thirdPartyId("thirdPartyId")
				.underlyingSymbol("underlyingSymbol")
				.lastTradeDate(LocalDate.now())
				.contractDefinition(ContractDefinition.builder().commissionFee(100).build())
				.symbol("symbol")
				.exchange(CoreEnum.ExchangeEnum.SHFE)
				.channelType(ChannelType.BIAN)
				.build();
		xyz.redtorch.pb.CoreField.ContractField contractField = contract.toContractField();
		assertEquals(contract.gatewayId(), contractField.getGatewayId());
		assertEquals(contract.contractId(), contractField.getContractId());
		assertEquals(contract.unifiedSymbol(), contractField.getUnifiedSymbol());
		assertEquals(contract.name(), contractField.getName());
		assertEquals(contract.fullName(), contractField.getFullName());
		assertEquals(contract.currency().getNumber(), contractField.getCurrency().getNumber());
		assertEquals(contract.productClass().getNumber(), contractField.getProductClass().getNumber());
		assertEquals(contract.multiplier(), contractField.getMultiplier());
		assertEquals(contract.priceTick(), contractField.getPriceTick());
		assertEquals(contract.longMarginRatio(), contractField.getLongMarginRatio());
		assertEquals(contract.shortMarginRatio(), contractField.getShortMarginRatio());
		assertEquals(contract.thirdPartyId(), contractField.getThirdPartyId());
		assertEquals(contract.underlyingSymbol(), contractField.getUnderlyingSymbol());
		assertEquals(contract.lastTradeDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER), contractField.getLastTradeDateOrContractMonth());
		assertEquals(contract.symbol(), contractField.getSymbol());
		assertEquals(contract.exchange().getNumber(), contractField.getExchange().getNumber());
		assertEquals(contract.channelType().toString(), contractField.getChannelType());
	}

	@Test 
	void testEquals() {
		Contract c1 = Contract.builder().symbol("rb2401").unifiedSymbol("rb2401@SHFE@FUTURES").name("螺纹钢2401").build();
		Contract c2 = Contract.builder().symbol("rb2401").unifiedSymbol("rb2401@SHFE@FUTURES").name("螺纹2401").build();
		assertEquals(c1, c2);
	}
	
	@Test
	void testHashEquals() {
		Contract c1 = Contract.builder().symbol("rb2401").unifiedSymbol("rb2401@SHFE@FUTURES").name("螺纹钢2401").build();
		Contract c2 = Contract.builder().symbol("rb2401").unifiedSymbol("rb2401@SHFE@FUTURES").name("螺纹2401").build();
		Map<Contract, String> map = new HashMap<>();
		map.put(c1, c1.name());
		assertThat(map).containsKey(c2);
	}
}
