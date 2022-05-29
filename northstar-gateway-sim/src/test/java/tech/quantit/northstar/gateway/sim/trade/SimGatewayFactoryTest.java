package tech.quantit.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.google.common.eventbus.EventBus;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.ISimAccountRepository;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.SimSettings;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;

class SimGatewayFactoryTest {

	@Test
	void test() {
		ISimAccountRepository accRepo = mock(ISimAccountRepository.class);
		SimMarket simMarket = mock(SimMarket.class);
		when(simMarket.getMarketEventBus()).thenReturn(mock(EventBus.class));
		SimGatewayFactory factory = new SimGatewayFactory(mock(FastEventEngine.class), simMarket, accRepo, mock(GlobalMarketRegistry.class), mock(IContractManager.class));
		GatewayDescription gd = GatewayDescription.builder().gatewayId("gatewayid").gatewayType(GatewayType.SIM)
				.gatewayUsage(GatewayUsage.TRADE).settings(new SimSettings()).build();
		assertThat(factory.newInstance(gd)).isNotNull();
		
		GatewayDescription gd2 = GatewayDescription.builder().gatewayId("gatewayid").gatewayType(GatewayType.SIM)
				.gatewayUsage(GatewayUsage.MARKET_DATA).settings(new SimSettings()).build();
		assertThat(factory.newInstance(gd2)).isNotNull();
	}

}
