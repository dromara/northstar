package tech.quantit.northstar.common;

import tech.quantit.northstar.common.model.Message;

public interface IMailSender {

	void send(Message message);
}
