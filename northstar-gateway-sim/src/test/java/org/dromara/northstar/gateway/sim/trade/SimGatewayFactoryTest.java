package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.IMarketCenter;
import org.junit.jupiter.api.Test;

class SimGatewayFactoryTest {

	@Test
	void test() {
		ISimAccountRepository accRepo = mock(ISimAccountRepository.class);
		SimGatewayFactory factory = new SimGatewayFactory(mock(FastEventEngine.class), accRepo, mock(IMarketCenter.class), mock(Map.class));
		GatewayDescription gd = GatewayDescription.builder().gatewayId("gatewayid").channelType(ChannelType.SIM)
				.gatewayUsage(GatewayUsage.TRADE).build();
		assertThat(factory.newInstance(gd)).isNotNull();
		
		GatewayDescription gd2 = GatewayDescription.builder().gatewayId("gatewayid").channelType(ChannelType.SIM)
				.gatewayUsage(GatewayUsage.MARKET_DATA).build();
		assertThat(factory.newInstance(gd2)).isNotNull();
	}

}
