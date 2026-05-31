package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

class SimContractTest {

	@Test
	void testContractConversion() {
		SimContract simContract = SimContract.builder()
				.contractId("sim9901@SHFE@FUTURES@SIM")
				.name("模拟合约9901")
				.fullName("模拟合约9901")
				.unifiedSymbol("sim9901@SHFE@FUTURES")
				.symbol("sim9901")
				.exchange(ExchangeEnum.SHFE)
				.productClass(ProductClassEnum.FUTURES)
				.currency(CurrencyEnum.CNY)
				.multiplier(10)
				.priceTick(1)
				.gatewayId("SIM")
				.build();
		
		ContractDefinition contractDef = ContractDefinition.builder()
				.exchange(ExchangeEnum.SHFE)
				.productClass(ProductClassEnum.FUTURES)
				.marginRatio(0.1)
				.build();
		simContract.setContractDefinition(contractDef);
		
		Contract contract = simContract.contract();
		
		assertThat(contract).isNotNull();
		assertThat(contract.contractId()).isEqualTo("sim9901@SHFE@FUTURES@SIM");
		assertThat(contract.name()).isEqualTo("模拟合约9901");
		assertThat(contract.unifiedSymbol()).isEqualTo("sim9901@SHFE@FUTURES");
		assertThat(contract.symbol()).isEqualTo("sim9901");
		assertThat(contract.exchange()).isEqualTo(ExchangeEnum.SHFE);
		assertThat(contract.productClass()).isEqualTo(ProductClassEnum.FUTURES);
		assertThat(contract.currency()).isEqualTo(CurrencyEnum.CNY);
		assertThat(contract.multiplier()).isEqualTo(10);
		assertThat(contract.priceTick()).isEqualTo(1);
		assertThat(contract.gatewayId()).isEqualTo("SIM");
		assertThat(contract.channelType()).isEqualTo(ChannelType.SIM);
		assertThat(contract.tradable()).isTrue();
		assertThat(contract.contractDefinition()).isEqualTo(contractDef);
		assertThat(contract.pricePrecision()).isEqualTo(0); // Math.log10(1/1) = 0
	}
	
	@Test
	void testContractConversionWithZeroPriceTick() {
		SimContract simContract = SimContract.builder()
				.contractId("sim9901@SHFE@FUTURES@SIM")
				.name("模拟合约9901")
				.symbol("sim9901")
				.exchange(ExchangeEnum.SHFE)
				.productClass(ProductClassEnum.FUTURES)
				.priceTick(0)
				.build();
		
		ContractDefinition contractDef = ContractDefinition.builder()
				.exchange(ExchangeEnum.SHFE)
				.productClass(ProductClassEnum.FUTURES)
				.build();
		simContract.setContractDefinition(contractDef);
		
		// Math.log10(1 / 0) results in Infinity, cast to int gives Integer.MAX_VALUE
		Contract contract = simContract.contract();
		assertThat(contract.pricePrecision()).isEqualTo(Integer.MAX_VALUE);
	}
	
	@Test
	void testContractWithoutDefinitionThrowsException() {
		SimContract simContract = SimContract.builder()
				.contractId("sim9901@SHFE@FUTURES@SIM")
				.build();
		
		assertThatThrownBy(() -> simContract.contract())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("没有合约定义信息");
	}
	
	@Test
	void testIdentifierIsLazyInitialized() {
		SimContract simContract = SimContract.builder()
				.contractId("sim9901@SHFE@FUTURES@SIM")
				.build();
		
		assertThat(simContract.identifier()).isNotNull();
		assertThat(simContract.identifier().value()).isEqualTo("sim9901@SHFE@FUTURES@SIM");
	}
	
	@Test
	void testChannelType() {
		SimContract simContract = SimContract.builder().build();
		assertThat(simContract.channelType()).isEqualTo(ChannelType.SIM);
	}
	
	@Test
	void testName() {
		SimContract simContract = SimContract.builder()
				.name("TestName")
				.build();
		assertThat(simContract.name()).isEqualTo("TestName");
	}
	
	@Test
	void testExchangeAndProductClass() {
		SimContract simContract = SimContract.builder()
				.exchange(ExchangeEnum.BINANCE)
				.productClass(ProductClassEnum.FUTURES)
				.build();
		assertThat(simContract.exchange()).isEqualTo(ExchangeEnum.BINANCE);
		assertThat(simContract.productClass()).isEqualTo(ProductClassEnum.FUTURES);
	}
	
	@Test
	void testDataSource() {
		SimContract simContract = SimContract.builder().build();
		assertThat(simContract.dataSource()).isNotNull();
	}
}
