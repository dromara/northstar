package tech.quantit.northstar.gateway.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.ChannelType;

@Component
public class GatewayChannelProvider {

	Map<String, ChannelType> typeMap = new HashMap<>();
	
	Map<String, GatewayFactory> factoryMap = new HashMap<>();
	
	public void addGatewayChannel(ChannelType gatewayType, GatewayFactory factory) {
		typeMap.put(gatewayType.name(), gatewayType);
		factoryMap.put(gatewayType.name(), factory);
	}
	
	public ChannelType valueOf(String name) {
		if(!typeMap.containsKey(name)) {
			throw new IllegalStateException("不存在该网关类型：" + name);
		}
		return typeMap.get(name);
	}
	
	public GatewayFactory getFactory(String name) {
		if(!factoryMap.containsKey(name)) {
			throw new IllegalStateException("不存在该网关类型：" + name);
		}	
		return factoryMap.get(name);
	}
	
	public Collection<ChannelType> getAll(){
		return typeMap.values();
	}
	
}
