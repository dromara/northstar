package tech.quantit.northstar.main.notification;

import tech.quantit.northstar.common.IMessageHandlerManager;
import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.GenericEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;

public class NotificationDispatcher extends AbstractEventHandler implements GenericEventHandler{

	private IMessageHandlerManager msgHandlerMgr;
	
	public NotificationDispatcher(IMessageHandlerManager messageHandlerManager) {
		msgHandlerMgr = messageHandlerManager;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return  true;
	}
	
	@Override
	protected void doHandle(NorthstarEvent e) {
		msgHandlerMgr.getHandlers().stream().forEach(sender -> sender.onEvent(e));
	}
	
}
