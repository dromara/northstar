package tech.xuanwu.northstar.gateway.api;

import tech.xuanwu.northstar.common.model.GatewayDescription;

public abstract class AbstractGatewayFactory {
	
	public abstract Gateway newInstance(GatewayDescription gatewayDescription);
}
