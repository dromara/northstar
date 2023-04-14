package org.dromara.northstar.gateway.api;

import org.dromara.northstar.common.model.GatewayDescription;

public interface GatewayFactory {
	
	public abstract Gateway newInstance(GatewayDescription gatewayDescription);
}
