package tech.quantit.northstar.gateway.api.domain;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.domain.time.CnFtComTradeTime1;
import tech.quantit.northstar.gateway.api.domain.time.GenericTradeTime;
import tech.quantit.northstar.gateway.api.domain.time.PeriodHelper;
import tech.quantit.northstar.gateway.api.domain.time.PeriodHelperFactory;
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
		PeriodHelperFactory phFactory = mock(PeriodHelperFactory.class);
		when(phFactory.newInstance(anyInt(), anyBoolean(), any(ContractField.class))).thenReturn(new PeriodHelper(60, new CnFtComTradeTime1(), LocalTime.of(21, 0)));
		registry = new GlobalMarketRegistry(mock(FastEventEngine.class), mock(Consumer.class), callback, mock(LatencyDetector.class), phFactory);
		
		NormalContract contract = mock(NormalContract.class);
		when(contract.contractField()).thenReturn(factory.makeContract("rb2210"));
		MarketGateway gateway = mock(MarketGateway.class);
		when(gateway.isConnected()).thenReturn(true);
		when(contract.gatewayType()).thenReturn("CTP");
		
		registry.register(contract);
		
		verify(callback).accept(any());
	}
	
	@Test
	void shouldNotSubscribeContract() {
		PeriodHelperFactory phFactory = mock(PeriodHelperFactory.class);
		when(phFactory.newInstance(anyInt(), anyBoolean(), any(ContractField.class))).thenReturn(new PeriodHelper(60, new CnFtComTradeTime1(), LocalTime.of(21, 0)));
		registry = new GlobalMarketRegistry(mock(FastEventEngine.class), mock(Consumer.class), mock(Consumer.class), mock(LatencyDetector.class), phFactory);
		
		IndexContract contract = mock(IndexContract.class);
		when(contract.contractField()).thenReturn(factory.makeContract("rb2210"));
		MarketGateway gateway = mock(MarketGateway.class);
		when(contract.indexTicker()).thenReturn(mock(IndexTicker.class));
		when(contract.gatewayType()).thenReturn("CTP");
		registry.register(contract);
		
		verify(gateway, times(0)).subscribe(any());
	}

	@Test
	void testRegisterMarketGateway() {
		PeriodHelperFactory phFactory = mock(PeriodHelperFactory.class);
		when(phFactory.newInstance(anyInt(), anyBoolean(), any(ContractField.class))).thenReturn(new PeriodHelper(60, new CnFtComTradeTime1(), LocalTime.of(21, 0)));
		registry = new GlobalMarketRegistry(mock(FastEventEngine.class), mock(Consumer.class), mock(Consumer.class), mock(LatencyDetector.class), phFactory);
		NormalContract contract = mock(NormalContract.class);
		MarketGateway gateway = mock(MarketGateway.class);
		when(contract.contractField()).thenReturn(factory.makeContract("rb2210"));
		when(gateway.gatewayType()).thenReturn("CTP");
		when(gateway.isConnected()).thenReturn(true);
		when(contract.gatewayType()).thenReturn("CTP");
		
		registry.register(contract);
		assertThatNoException();
	}

	@Test
	void testDispatch() {
		PeriodHelperFactory phFactory = mock(PeriodHelperFactory.class);
		when(phFactory.newInstance(anyInt(), anyBoolean(), any(ContractField.class))).thenReturn(new PeriodHelper(60, new GenericTradeTime(), LocalTime.of(21, 0)));
		registry = new GlobalMarketRegistry(mock(FastEventEngine.class), mock(Consumer.class), mock(Consumer.class), mock(LatencyDetector.class), phFactory);
		TickField tick = factory.makeTickField("rb2210", 2000);
		NormalContract contract = mock(NormalContract.class);
		when(contract.contractField()).thenReturn(factory.makeContract("rb2210"));
		MarketGateway gateway = mock(MarketGateway.class);
		when(gateway.gatewayType()).thenReturn("CTP");
		when(contract.gatewayType()).thenReturn("CTP");
		when(contract.unifiedSymbol()).thenReturn(tick.getUnifiedSymbol());
		registry.register(contract);
		registry.dispatch(tick);
	}

}
