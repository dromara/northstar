package tech.quantit.northstar.main.notification;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import tech.quantit.northstar.common.MessageHandler;
import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.GenericEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;

public class NotificationDispatcher extends AbstractEventHandler implements GenericEventHandler{

	@Autowired
	private List<MessageHandler> handlerList;
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return  true;
	}
	
	@Override
	protected void doHandle(NorthstarEvent e) {
		handlerList.stream().forEach(sender -> sender.onEvent(e));
	}
	
}
