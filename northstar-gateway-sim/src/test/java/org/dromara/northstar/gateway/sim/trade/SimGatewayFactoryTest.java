package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.dromara.northstar.gateway.sim.trade.SimGatewayFactory;
import org.dromara.northstar.gateway.sim.trade.SimMarket;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.gateway.api.IMarketCenter;

class SimGatewayFactoryTest {

	@Test
	void test() {
		ISimAccountRepository accRepo = mock(ISimAccountRepository.class);
		SimMarket simMarket = mock(SimMarket.class);
		SimGatewayFactory factory = new SimGatewayFactory(mock(FastEventEngine.class), simMarket, accRepo, mock(IMarketCenter.class));
		GatewayDescription gd = GatewayDescription.builder().gatewayId("gatewayid").channelType(ChannelType.SIM)
				.gatewayUsage(GatewayUsage.TRADE).build();
		assertThat(factory.newInstance(gd)).isNotNull();
		
		GatewayDescription gd2 = GatewayDescription.builder().gatewayId("gatewayid").channelType(ChannelType.SIM)
				.gatewayUsage(GatewayUsage.MARKET_DATA).build();
		assertThat(factory.newInstance(gd2)).isNotNull();
	}

}
