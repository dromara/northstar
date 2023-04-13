package tech.quantit.northstar.gateway.api.domain.mktdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.domain.contract.ContractDefinition;
import tech.quantit.northstar.gateway.api.domain.contract.Instrument;
import tech.quantit.northstar.gateway.api.domain.time.GenericTradeTime;
import tech.quantit.northstar.gateway.api.domain.time.TradeTimeDefinition;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

class MarketCenterTest {

	String GATEWAY_ID = "testGateway";
	TestFieldFactory factory = new TestFieldFactory(GATEWAY_ID);
	
	MarketCenter center;
	
	Instrument ins1 = new TestContract("rb2305", "rb2305@SHFE@FUTURES", ProductClassEnum.FUTURES, ExchangeEnum.SHFE);
	
	Instrument ins2 = new TestContract("sc2403", "sc2403@INE@FUTURES@PLAYBACK", ProductClassEnum.FUTURES, ExchangeEnum.INE);
	
	Instrument ins3 = new TestContract("rb2305C5000", "rb2305C5000@SHFE@OPTIONS", ProductClassEnum.OPTION, ExchangeEnum.SHFE);
	
	@BeforeEach
	void prepare() {
		ContractDefinition def1 = ContractDefinition.builder()
				.name("RB指数")
				.symbolPattern(Pattern.compile("rb\\d+@.+@FUTURES"))
				.productClass(ProductClassEnum.FUTURES)
				.exchange(ExchangeEnum.SHFE)
				.build();
		ContractDefinition def2 = ContractDefinition.builder()
				.name("RB期权链")
				.symbolPattern(Pattern.compile("rb.+@.+@OPTIONS"))
				.productClass(ProductClassEnum.OPTION)
				.exchange(ExchangeEnum.SHFE)
				.build();
		ContractDefinition def3 = ContractDefinition.builder()
				.name("SC指数")
				.symbolPattern(Pattern.compile("sc[0-9]{3,4}@.+"))
				.productClass(ProductClassEnum.FUTURES)
				.exchange(ExchangeEnum.INE)
				.build();
		center = new MarketCenter(List.of(def1, def2, def3), mock(FastEventEngine.class));
		
	}
	
	@Test
	void testAddInstument() {
		MarketGateway gateway = mock(MarketGateway.class);
		when(gateway.gatewayId()).thenReturn(GATEWAY_ID);
		center.addInstrument(ins1);
		center.addInstrument(ins2);
		center.addInstrument(ins3);
		
		assertThat(center.getContracts(GATEWAY_ID)).hasSize(3);
		assertThat(center.getContracts("")).hasSize(3);
	}
	
	@Test
	void testFindContract() {
		MarketGateway gateway = mock(MarketGateway.class);
		when(gateway.gatewayId()).thenReturn(GATEWAY_ID);
		center.addInstrument(ins1);
		center.addInstrument(ins2);
		center.addInstrument(ins3);
		center.loadContractGroup(ChannelType.SIM);
		
		assertThat(center.getContract(GATEWAY_ID, "rb2305").identifier()).isEqualTo(Identifier.of("rb2305@SHFE@FUTURES"));
		assertThat(center.getContract(GATEWAY_ID, "rb2305@SHFE@FUTURES").identifier()).isEqualTo(Identifier.of("rb2305@SHFE@FUTURES"));
		assertThat(center.getContract(Identifier.of("rb2305@SHFE@FUTURES"))).isEqualTo(center.getContract(GATEWAY_ID, "rb2305"));
	}

	@Test
	void testAggregateContract() {
		MarketGateway gateway = mock(MarketGateway.class);
		when(gateway.gatewayId()).thenReturn(GATEWAY_ID);
		center.addInstrument(ins1);
		center.addInstrument(ins2);
		center.addInstrument(ins3);
		center.loadContractGroup(ChannelType.SIM);
		
		assertThat(center.getContracts(GATEWAY_ID)).hasSize(6);
		assertThat(center.getContracts("")).hasSize(6);
	}
	
	@Test 
	void testTick() {
		MarketGateway gateway = mock(MarketGateway.class);
		when(gateway.gatewayId()).thenReturn(GATEWAY_ID);
		center.addInstrument(ins1);
		center.addInstrument(ins2);
		center.addInstrument(ins3);
		center.loadContractGroup(ChannelType.SIM);
		
		TickField t = factory.makeTickField("rb2305", 5000);
		assertDoesNotThrow(() -> {
			center.onTick(t);
		});
	}
	
	@AllArgsConstructor
	class TestContract implements Instrument {
		
		private String symbol;
		private String unifiedSymbol;
		private ProductClassEnum productClass;
		private ExchangeEnum exchange;

		@Override
		public String name() {
			return symbol;
		}

		@Override
		public Identifier identifier() {
			return Identifier.of(unifiedSymbol);
		}

		@Override
		public ProductClassEnum productClass() {
			return productClass;
		}

		@Override
		public ExchangeEnum exchange() {
			return exchange;
		}

		@Override
		public TradeTimeDefinition tradeTimeDefinition() {
			return new GenericTradeTime();
		}

		@Override
		public ChannelType channelType() {
			return ChannelType.SIM;
		}

		@Override
		public void setContractDefinition(ContractDefinition contractDef) {
		}

		@Override
		public ContractField contractField() {
			return ContractField.newBuilder()
					.setSymbol(symbol)
					.setUnifiedSymbol(unifiedSymbol)
					.setExchange(ExchangeEnum.SHFE)
					.setProductClass(ProductClassEnum.FUTURES)
					.setContractId(identifier().value())
					.setUnderlyingSymbol(symbol.replaceAll("([A-z]+[0-9]+)[CP][0-9]+", "$1"))
					.setGatewayId(GATEWAY_ID)
					.setName(name())
					.build();
		}
		
	}
}
