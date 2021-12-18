package tech.quantit.northstar.gateway.api;

import tech.quantit.northstar.common.model.GatewayDescription;

public interface GatewayFactory {
	
	public abstract Gateway newInstance(GatewayDescription gatewayDescription);
}
