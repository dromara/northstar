package tech.quantit.northstar.gateway.api;

import tech.quantit.northstar.common.model.GatewayDescription;

public abstract class AbstractGatewayFactory {
	
	public abstract Gateway newInstance(GatewayDescription gatewayDescription);
}
