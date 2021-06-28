package tech.xuanwu.northstar.factories;

import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.gateway.api.Gateway;

public abstract class AbstractGatewayFactory {
	
	public abstract Gateway newInstance(GatewayDescription gatewayDescription);
}
