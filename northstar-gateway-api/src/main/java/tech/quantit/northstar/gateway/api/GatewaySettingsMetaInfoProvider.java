package tech.quantit.northstar.gateway.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.model.ComponentField;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.GatewaySettings;

@Component
public class GatewaySettingsMetaInfoProvider {
	
	/* gatewayType -> settings */
	Map<String, GatewaySettings> settingsMap = new HashMap<>();
	
	public void addSettings(String gatewayType, GatewaySettings settings) {
		settingsMap.put(gatewayType, settings);
	}
	
	public Collection<ComponentField> getSettings(String gatewayType) {
		return  ((DynamicParams)settingsMap.get(gatewayType)).getMetaInfo().values();
	}
}
