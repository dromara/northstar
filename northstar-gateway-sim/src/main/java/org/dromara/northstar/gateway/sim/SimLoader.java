package org.dromara.northstar.gateway.sim;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.sim.trade.SimGatewayFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(0)
@Component
public class SimLoader implements CommandLineRunner{

	@Autowired
	private GatewayMetaProvider gatewayMetaProvider;
	
	@Autowired
	private SimGatewayFactory simGatewayFactory;
	
	@Override
	public void run(String... args) throws Exception {
		gatewayMetaProvider.add(ChannelType.SIM, null, simGatewayFactory);
	}

}
