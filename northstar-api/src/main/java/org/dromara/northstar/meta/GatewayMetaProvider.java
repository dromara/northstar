package org.dromara.northstar.meta;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.gateway.GatewayFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GatewayMetaProvider {
	
	private Map<ChannelType, GatewayFactory> factoryMap = new EnumMap<>(ChannelType.class);
	
	public GatewayMetaProvider(List<GatewayFactory> factoryList) {
		for(GatewayFactory factory : factoryList) {
			factoryMap.put(factory.channelType(), factory);
		}
	}
	
	public Map<ChannelType, Collection<ComponentField>> getSettings() {
		return  factoryMap.entrySet()
				.stream()
				.filter(e -> e.getValue().gatewaySettings() != null)
				.collect(Collectors.toMap(Map.Entry::getKey, e -> ((DynamicParams)e.getValue().gatewaySettings()).getMetaInfo()));
	}
	
	public GatewayFactory getFactory(ChannelType channelType) {
		if(!factoryMap.containsKey(channelType)) {
			throw new IllegalStateException("没有该渠道的网关类型：" + channelType);
		}	
		return factoryMap.get(channelType);
	}
	
	public List<ChannelType> availableChannel() {
		return factoryMap.keySet().stream().toList();
	}
	
}
