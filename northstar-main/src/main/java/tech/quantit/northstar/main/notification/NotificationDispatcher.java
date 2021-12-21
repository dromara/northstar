package tech.quantit.northstar.main.notification;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.MessageHandler;
import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.GenericEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;

@Component
public class NotificationDispatcher extends AbstractEventHandler implements GenericEventHandler{

	@Autowired
	private List<MessageHandler> senderList;
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return  true;
	}
	
	@Override
	protected void doHandle(NorthstarEvent e) {
		senderList.stream().forEach(sender -> sender.onEvent(e));
	}
	
}
