package tech.quantit.northstar.gateway.api;

import java.util.EnumMap;

import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.constant.ChannelType;

@Component
public class GatewayChannelProvider {

	EnumMap<ChannelType, GatewayFactory> factoryMap = new EnumMap<>(ChannelType.class);
	
	public void addGatewayChannel(ChannelType gatewayType, GatewayFactory factory) {
		factoryMap.put(gatewayType, factory);
	}
	
	public GatewayFactory getFactory(ChannelType gatewayType) {
		if(!factoryMap.containsKey(gatewayType)) {
			throw new IllegalStateException("不存在该网关类型：" + gatewayType);
		}	
		return factoryMap.get(gatewayType);
	}
	
}
