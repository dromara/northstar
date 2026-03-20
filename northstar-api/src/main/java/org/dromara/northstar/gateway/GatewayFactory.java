package org.dromara.northstar.gateway;

import org.dromara.northstar.common.GatewaySettings;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.GatewayDescription;

public interface GatewayFactory {
	
	Gateway newInstance(GatewayDescription gatewayDescription);
	
	GatewayDescription enhance(GatewayDescription gatewayDescription);
	
	GatewaySettings gatewaySettings();
	
	ChannelType channelType();
}
