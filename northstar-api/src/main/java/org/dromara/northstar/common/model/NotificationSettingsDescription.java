package org.dromara.northstar.common.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.event.NorthstarEventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsDescription {

	private List<NorthstarEventType> subscribedEvents;
	
	private Map<String, Collection<ComponentField>> settingsMap;
	
}
