package org.dromara.northstar.gateway.mktdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.IContractDefinitionProvider;
import org.dromara.northstar.gateway.Instrument;
import org.dromara.northstar.gateway.MarketGateway;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

class MarketCenterTest {
	
	ContractDefinition cd = ContractDefinition.builder().name("test").exchange(ExchangeEnum.DCE).productClass(ProductClassEnum.FUTURES).symbolPattern(Pattern.compile("test")).build();
	IContractDefinitionProvider pvider = new IContractDefinitionProvider() {
		@Override
		public List<ContractDefinition> get() {
			return List.of(cd);
		}
	};
	
	MarketCenter mktCenter = new MarketCenter(mock(FastEventEngine.class), List.of(pvider), new HashMap<>());
	
	@Test
	void testAddAndGetDefinitions() {
		assertThat(mktCenter.getDefinition(ExchangeEnum.DCE, ProductClassEnum.FUTURES, "test")).containsSame(cd);
	}

	@Test
	void testAddInstrument() {
		DummyInstrument ins = new DummyInstrument();
		assertDoesNotThrow(() -> {
			mktCenter.addInstrument(ins);
		});
	}
	
	@Test
	void testLoadContractGroup() {
		assertDoesNotThrow(() -> {
			mktCenter.loadContractGroup(ChannelType.CTP);
		});
	}

	@Test
	void testGetContractIdentifier() {
		DummyInstrument ins = new DummyInstrument();
		mktCenter.addInstrument(ins);
		assertDoesNotThrow(() -> {
			mktCenter.getContract(Identifier.of("test"));
		});
	}

	@Test
	void testGetContractChannelTypeString() {
		DummyInstrument ins = new DummyInstrument();
		mktCenter.addInstrument(ins);
		assertThat(mktCenter.getContract(ChannelType.CTP, "test").contract()).isEqualTo(ins.contract());
	}

	@Test
	void testGetContractsString() {
		DummyInstrument ins = new DummyInstrument();
		mktCenter.addInstrument(ins);
		assertThat(mktCenter.getContracts(ChannelType.CTP.toString())).hasSize(1);
	}

	@Test
	void testGetContractsChannelType() {
		DummyInstrument ins = new DummyInstrument();
		mktCenter.addInstrument(ins);
		assertThat(mktCenter.getContracts(ChannelType.CTP)).hasSize(1);
	}

	@Test
	void testTick() {
		DummyInstrument ins = new DummyInstrument();
		mktCenter.addInstrument(ins);
		Contract c = ins.contract();
		Tick tick = Tick.builder().contract(c).channelType(ChannelType.CTP).build();
		
		mktCenter.onTick(tick);
		assertThat(mktCenter.lastTick(c)).hasValue(tick);
	}

	@Test
	void testAddAndGetGateway() {
		MarketGateway gateway = new DummyGateway(GatewayDescription.builder().gatewayId("test").channelType(ChannelType.CTP).build());
		mktCenter.addGateway(gateway);
		assertThat(mktCenter.getGateway(ChannelType.CTP)).isNotNull();
	}

	class DummyInstrument implements Instrument{

		@Override
		public String name() {
			return "test";
		}

		@Override
		public Identifier identifier() {
			return Identifier.of("test");
		}

		@Override
		public ProductClassEnum productClass() {
			return ProductClassEnum.FUTURES;
		}

		@Override
		public ExchangeEnum exchange() {
			return ExchangeEnum.DCE;
		}

		@Override
		public ChannelType channelType() {
			return ChannelType.CTP;
		}

		@Override
		public void setContractDefinition(ContractDefinition contractDef) {
		}

		@Override
		public Contract contract() {
			return Contract.builder().unifiedSymbol("test").symbol("test").gatewayId("CTP").build();
		}
		
	}
	
	class DummyGateway extends MarketGateway{
		
		
		protected DummyGateway(GatewayDescription description) {
			super(description);
		}

		@Override
		public void connect() {}

		@Override
		public void disconnect() {}

		@Override
		public boolean subscribe(Contract contract) {
			return false;
		}

		@Override
		public boolean unsubscribe(Contract contract) {
			return false;
		}

		@Override
		public boolean isActive() {
			return false;
		}
	}
}
