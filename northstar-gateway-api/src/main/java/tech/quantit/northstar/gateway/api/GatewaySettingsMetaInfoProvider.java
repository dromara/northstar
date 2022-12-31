package tech.quantit.northstar.gateway.api;

import java.util.Collection;
import java.util.Map;

import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.model.ComponentField;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.GatewaySettings;

public class GatewaySettingsMetaInfoProvider {
	
	/* gatewayType -> settings */
	Map<ChannelType, GatewaySettings> settingsMap;
	
	public GatewaySettingsMetaInfoProvider(Map<ChannelType, GatewaySettings> settingsMap) {
		this.settingsMap = settingsMap;
	}
	
	public void addSettings(ChannelType channelType, GatewaySettings settings) {
		settingsMap.put(channelType, settings);
	}
	
	public Collection<ComponentField> getSettings(ChannelType channelType) {
		return  ((DynamicParams)settingsMap.get(channelType)).getMetaInfo().values();
	}
}
