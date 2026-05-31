package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.gateway.Instrument;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

class SimContractGeneratorTest {

	@Test
	void testGetContract() {
		SimContractGenerator gen = new SimContractGenerator("test");
		Instrument ins = gen.getContract();
		
		assertThat(ins).isNotNull();
		assertThat(ins.name()).isEqualTo("模拟合约9901");
		assertThat(ins.identifier().value()).isEqualTo("sim9901@SHFE@FUTURES@test");
		assertThat(ins.exchange()).isEqualTo(ExchangeEnum.SHFE);
		assertThat(ins.productClass()).isEqualTo(ProductClassEnum.FUTURES);
		assertThat(ins.channelType()).isEqualTo(ChannelType.SIM);
		
		ins.setContractDefinition(ContractDefinition.builder().exchange(ExchangeEnum.SHFE).productClass(ProductClassEnum.FUTURES).build());
		Contract contract = ins.contract();
		assertThat(contract.symbol()).isEqualTo("sim9901");
		assertThat(contract.unifiedSymbol()).isEqualTo("sim9901@SHFE@FUTURES");
		assertThat(contract.multiplier()).isEqualTo(10);
		assertThat(contract.priceTick()).isEqualTo(1);
		assertThat(contract.currency()).isEqualTo(CurrencyEnum.CNY);
		assertThat(contract.gatewayId()).isEqualTo("SIM");
	}

	@Test
	void testGetContract2() {
		SimContractGenerator gen = new SimContractGenerator("test");
		Instrument ins = gen.getContract2();
		
		assertThat(ins).isNotNull();
		assertThat(ins.name()).isEqualTo("模拟合约9902");
		assertThat(ins.identifier().value()).isEqualTo("sim9902@SHFE@FUTURES@test");
		
		ins.setContractDefinition(ContractDefinition.builder().exchange(ExchangeEnum.SHFE).productClass(ProductClassEnum.FUTURES).build());
		Contract contract = ins.contract();
		assertThat(contract.symbol()).isEqualTo("sim9902");
		assertThat(contract.multiplier()).isEqualTo(10);
		assertThat(contract.priceTick()).isEqualTo(1);
	}

	@Test
	void testGetContract3() {
		SimContractGenerator gen = new SimContractGenerator("test");
		Instrument ins = gen.getContract3();
		
		assertThat(ins).isNotNull();
		assertThat(ins.name()).isEqualTo("模拟合约9903");
		assertThat(ins.identifier().value()).isEqualTo("sim9903@BINANCE@FUTURES@test");
		assertThat(ins.exchange()).isEqualTo(ExchangeEnum.BINANCE);
		
		ins.setContractDefinition(ContractDefinition.builder().exchange(ExchangeEnum.SHFE).productClass(ProductClassEnum.FUTURES).build());
		ins.setContractDefinition(ContractDefinition.builder().exchange(ExchangeEnum.BINANCE).productClass(ProductClassEnum.FUTURES).build());
		Contract contract = ins.contract();
		assertThat(contract.symbol()).isEqualTo("sim9903");
		assertThat(contract.multiplier()).isEqualTo(1);
		assertThat(contract.priceTick()).isEqualTo(1.0E-7);
		assertThat(contract.currency()).isEqualTo(CurrencyEnum.USDT);
		assertThat(contract.gatewayId()).isEqualTo("SIM");
	}
	
	@Test
	void testAllContractsHaveSameGatewayId() {
		SimContractGenerator gen = new SimContractGenerator("MY_GATEWAY");
		
		Instrument ins1 = gen.getContract();
		ins1.setContractDefinition(ContractDefinition.builder().exchange(ExchangeEnum.SHFE).productClass(ProductClassEnum.FUTURES).build());
		assertThat(ins1.contract().gatewayId()).isEqualTo("SIM");
		Instrument ins2 = gen.getContract2();
		ins2.setContractDefinition(ContractDefinition.builder().exchange(ExchangeEnum.SHFE).productClass(ProductClassEnum.FUTURES).build());
		assertThat(ins2.contract().gatewayId()).isEqualTo("SIM");
		Instrument ins3 = gen.getContract3();
		ins3.setContractDefinition(ContractDefinition.builder().exchange(ExchangeEnum.BINANCE).productClass(ProductClassEnum.FUTURES).build());
		assertThat(ins3.contract().gatewayId()).isEqualTo("SIM");
	}
	
	@Test
	void testLastTradeDateIsMax() {
		SimContractGenerator gen = new SimContractGenerator("test");
		
		Instrument ins1 = gen.getContract();
		ins1.setContractDefinition(ContractDefinition.builder().exchange(ExchangeEnum.SHFE).productClass(ProductClassEnum.FUTURES).build());
		assertThat(ins1.contract().lastTradeDate()).isEqualTo(java.time.LocalDate.MAX);
		Instrument ins2 = gen.getContract2();
		ins2.setContractDefinition(ContractDefinition.builder().exchange(ExchangeEnum.SHFE).productClass(ProductClassEnum.FUTURES).build());
		assertThat(ins2.contract().lastTradeDate()).isEqualTo(java.time.LocalDate.MAX);
		Instrument ins3 = gen.getContract3();
		ins3.setContractDefinition(ContractDefinition.builder().exchange(ExchangeEnum.BINANCE).productClass(ProductClassEnum.FUTURES).build());
		assertThat(ins3.contract().lastTradeDate()).isEqualTo(java.time.LocalDate.MAX);
	}
}
