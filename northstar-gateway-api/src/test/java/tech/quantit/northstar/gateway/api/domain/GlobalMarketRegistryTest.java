package tech.quantit.northstar.gateway.api.domain;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.gateway.api.MarketGateway;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

@SuppressWarnings("unchecked")
class GlobalMarketRegistryTest {
	
	TestFieldFactory factory = new TestFieldFactory("gateway");
	GlobalMarketRegistry registry;
	
	@Test
	void shouldSubscribeContract() {
		Consumer<ContractField> callback = mock(Consumer.class);
		registry = new GlobalMarketRegistry(mock(FastEventEngine.class), mock(Consumer.class), callback);
		
		NormalContract contract = mock(NormalContract.class);
		when(contract.contractField()).thenReturn(factory.makeContract("rb2210"));
		MarketGateway gateway = mock(MarketGateway.class);
		when(gateway.isConnected()).thenReturn(true);
		when(contract.gatewayType()).thenReturn(GatewayType.CTP);
		when(contract.barGenerator()).thenReturn(mock(BarGenerator.class));
		
		registry.register(contract);
		
		verify(callback).accept(any());
	}
	
	@Test
	void shouldNotSubscribeContract() {
		registry = new GlobalMarketRegistry(mock(FastEventEngine.class), mock(Consumer.class), mock(Consumer.class));
		
		IndexContract contract = mock(IndexContract.class);
		when(contract.contractField()).thenReturn(factory.makeContract("rb2210"));
		MarketGateway gateway = mock(MarketGateway.class);
		when(contract.indexTicker()).thenReturn(mock(IndexTicker.class));
		when(contract.gatewayType()).thenReturn(GatewayType.CTP);
		when(contract.barGenerator()).thenReturn(mock(BarGenerator.class));
		registry.register(contract);
		
		verify(gateway, times(0)).subscribe(any());
	}

	@Test
	void testRegisterMarketGateway() {
		registry = new GlobalMarketRegistry(mock(FastEventEngine.class), mock(Consumer.class), mock(Consumer.class));
		NormalContract contract = mock(NormalContract.class);
		MarketGateway gateway = mock(MarketGateway.class);
		when(contract.contractField()).thenReturn(factory.makeContract("rb2210"));
		when(gateway.gatewayType()).thenReturn(GatewayType.CTP);
		when(gateway.isConnected()).thenReturn(true);
		when(contract.gatewayType()).thenReturn(GatewayType.CTP);
		when(contract.barGenerator()).thenReturn(mock(BarGenerator.class));
		
		registry.register(contract);
		assertThatNoException();
	}

	@Test
	void testDispatch() {
		registry = new GlobalMarketRegistry(mock(FastEventEngine.class), mock(Consumer.class), mock(Consumer.class));
		TickField tick = factory.makeTickField("rb2210", 2000);
		NormalContract contract = mock(NormalContract.class);
		when(contract.contractField()).thenReturn(factory.makeContract("rb2210"));
		MarketGateway gateway = mock(MarketGateway.class);
		when(gateway.gatewayType()).thenReturn(GatewayType.CTP);
		when(contract.gatewayType()).thenReturn(GatewayType.CTP);
		when(contract.unifiedSymbol()).thenReturn(tick.getUnifiedSymbol());
		BarGenerator barGen = mock(BarGenerator.class);
		when(contract.barGenerator()).thenReturn(barGen);
		registry.register(contract);
		registry.dispatch(tick);
		
		verify(barGen).update(any());
	}

}
