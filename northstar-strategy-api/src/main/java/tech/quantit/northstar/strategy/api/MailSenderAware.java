package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.common.IMailSender;
import tech.quantit.northstar.common.model.Message;

public interface MailSenderAware {

	void setMailSender(IMailSender sender);
	
	void sendMessage(Message msg);
}
