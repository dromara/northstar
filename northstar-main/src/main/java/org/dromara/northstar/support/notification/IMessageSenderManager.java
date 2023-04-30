package org.dromara.northstar.support.notification;

import org.dromara.northstar.strategy.IMessageSender;

public interface IMessageSenderManager {

	IMessageSender getSender();
}
