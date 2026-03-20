package org.dromara.northstar.meta;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.dromara.northstar.common.MessageSenderFactory;
import org.dromara.northstar.common.MessageSenderSettings;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.DynamicParams;
import org.springframework.stereotype.Component;

@Component
public class MsgSenderMetaProvider {

	private Map<String, MessageSenderSettings> settingsMap = new HashMap<>();
	
	private Map<String, MessageSenderFactory> factoryMap = new HashMap<>();
	
	public Map<String, Collection<ComponentField>> msgSenderSettingsMeta(){
		return settingsMap.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> ((DynamicParams) e.getValue()).getMetaInfo()));
	}
	
	public void register(String messageType, MessageSenderSettings settings, MessageSenderFactory factory) {
		settingsMap.put(messageType, settings);
		factoryMap.put(messageType, factory);
	}
	
	public MessageSenderFactory getMessageSenderFactory(String name) {
		return factoryMap.get(name);
	}
}
