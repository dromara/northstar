package org.dromara.northstar.gateway.sim;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.sim.trade.SimGatewayFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimConfig{

	@Bean
	SimGatewayFactory simGatewayFactory(FastEventEngine feEngine, ISimAccountRepository simAccountRepo, IMarketCenter marketCenter) {
		return new SimGatewayFactory(feEngine, simAccountRepo, marketCenter);
	}
	
}
