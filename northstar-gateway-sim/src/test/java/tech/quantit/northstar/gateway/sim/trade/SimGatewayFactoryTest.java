package tech.quantit.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.SimSettings;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountRepository;

class SimGatewayFactoryTest {

	@Test
	void test() {
		SimAccountRepository accRepo = mock(SimAccountRepository.class);
		SimGatewayFactory factory = new SimGatewayFactory(mock(FastEventEngine.class), mock(SimMarket.class), accRepo);
		GatewayDescription gd = GatewayDescription.builder().gatewayId("gatewayid").gatewayType(GatewayType.SIM)
				.gatewayUsage(GatewayUsage.TRADE).settings(new SimSettings()).build();
		assertThat(factory.newInstance(gd)).isNotNull();
		
		GatewayDescription gd2 = GatewayDescription.builder().gatewayId("gatewayid").gatewayType(GatewayType.SIM)
				.gatewayUsage(GatewayUsage.MARKET_DATA).settings(new SimSettings()).build();
		assertThat(factory.newInstance(gd2)).isNotNull();
	}

}
