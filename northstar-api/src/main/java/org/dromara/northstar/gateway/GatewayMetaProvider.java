package org.dromara.northstar.gateway;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.dromara.northstar.common.GatewaySettings;
import org.dromara.northstar.common.IDataServiceManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.DynamicParams;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GatewayMetaProvider {
	
	/* gatewayType -> settings */
	private Map<ChannelType, GatewaySettings> settingsMap = new EnumMap<>(ChannelType.class);
	
	private Map<ChannelType, GatewayFactory> factoryMap = new EnumMap<>(ChannelType.class);
	
	private Map<ChannelType, IDataServiceManager> mdRepoMap = new EnumMap<>(ChannelType.class);
	
	
	public Collection<ComponentField> getSettings(ChannelType channelType) {
		return  ((DynamicParams)settingsMap.get(channelType)).getMetaInfo().values();
	}
	
	public GatewayFactory getFactory(ChannelType channelType) {
		if(!factoryMap.containsKey(channelType)) {
			throw new IllegalStateException("没有该渠道的网关类型：" + channelType);
		}	
		return factoryMap.get(channelType);
	}
	
	public IDataServiceManager getMarketDataRepo(ChannelType channelType) {
		if(mdRepoMap.containsKey(channelType)) {
			return mdRepoMap.get(channelType);
		}
		throw new IllegalStateException("没有该渠道的数据来源：" + channelType);
	}
	
	public void add(ChannelType channelType, GatewaySettings settings, GatewayFactory factory, IDataServiceManager dsMgr) {
		log.info("注册 [{}] 渠道元信息", channelType);
		if(Objects.nonNull(settings))
			settingsMap.put(channelType, settings);
		if(Objects.nonNull(factory))
			factoryMap.put(channelType, factory);
		if(Objects.nonNull(dsMgr))
			mdRepoMap.put(channelType, dsMgr);
	}
	
	public List<ChannelType> availableChannel() {
		Set<ChannelType> channels = new HashSet<>();
		channels.addAll(factoryMap.keySet());
		channels.addAll(settingsMap.keySet());
		return channels.stream().sorted().toList();
	}
}
