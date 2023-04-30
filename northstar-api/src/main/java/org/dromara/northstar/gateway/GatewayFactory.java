package org.dromara.northstar.gateway;

import org.dromara.northstar.common.model.GatewayDescription;

public interface GatewayFactory {
	
	public abstract Gateway newInstance(GatewayDescription gatewayDescription);
}
