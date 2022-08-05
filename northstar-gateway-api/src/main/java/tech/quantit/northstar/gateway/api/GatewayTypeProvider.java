package tech.quantit.northstar.gateway.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.GatewayType;

@Component
public class GatewayTypeProvider {

	Map<String, GatewayType> typeMap = new HashMap<>();
	
	Map<String, GatewayFactory> factoryMap = new HashMap<>();
	
	public void addGatewayType(GatewayType gatewayType, GatewayFactory factory) {
		typeMap.put(gatewayType.name(), gatewayType);
		factoryMap.put(gatewayType.name(), factory);
	}
	
	public GatewayType valueOf(String name) {
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
	
	public Collection<GatewayType> getAll(){
		return typeMap.values();
	}
	
}
