package tech.quantit.northstar.gateway.binance;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;

public class BinanceGatewayFactory implements GatewayFactory{

	private FastEventEngine fastEventEngine;
	
	private GlobalMarketRegistry registry;
	
	public BinanceGatewayFactory(FastEventEngine fastEventEngine, GlobalMarketRegistry registry) {
		this.fastEventEngine = fastEventEngine;
		this.registry = registry;
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		// TODO Auto-generated method stub
		return null;
	}

}
