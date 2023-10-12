package org.dromara.northstar.data;

import java.util.List;

import org.dromara.northstar.common.MessageSenderSettings;
import org.dromara.northstar.common.event.NorthstarEventType;

/**
 * 消息发送器配置持久化
 * @author KevinHuangwl
 *
 */
public interface IMessageSenderRepository {

	void save(MessageSenderSettings settings);
	
	MessageSenderSettings get(Class<?> settingsClz);
	
	void save(List<NorthstarEventType> subEvents);
	
	List<NorthstarEventType> getSubEvents();
}
