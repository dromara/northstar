package org.dromara.northstar.support.notification;

import java.util.List;

import org.dromara.northstar.strategy.IMessageSender;

public interface IMessageSenderManager {

	IMessageSender getSender(boolean inheritSubscribers);
	
	List<String> getSubscribers();
}
