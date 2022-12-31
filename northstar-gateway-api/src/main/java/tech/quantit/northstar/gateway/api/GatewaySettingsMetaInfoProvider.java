package tech.quantit.northstar.gateway.api;

import java.util.Collection;
import java.util.EnumMap;

import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.model.ComponentField;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.GatewaySettings;

@Component
public class GatewaySettingsMetaInfoProvider {
	
	/* gatewayType -> settings */
	EnumMap<ChannelType, GatewaySettings> settingsMap = new EnumMap<>(ChannelType.class);
	
	public void addSettings(ChannelType channelType, GatewaySettings settings) {
		settingsMap.put(channelType, settings);
	}
	
	public Collection<ComponentField> getSettings(ChannelType channelType) {
		return  ((DynamicParams)settingsMap.get(channelType)).getMetaInfo().values();
	}
}
