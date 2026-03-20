package org.dromara.northstar.support.utils;

import org.dromara.northstar.support.msg.MessageSenderManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageSenderFacade {
	
	@Autowired
	private MessageSenderManager senderMgr;
	
	public void send(String title, String content) {
		senderMgr.findAll().stream().forEach(sender -> sender.asyncSend(title, content));
	}
}
