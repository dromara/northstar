package org.dromara.northstar.common;

import org.dromara.northstar.common.model.NotificationSettingsDescription;
import org.dromara.northstar.strategy.IMessageSender;

public interface MessageSenderFactory {

	IMessageSender newInstance(NotificationSettingsDescription settings);
}
