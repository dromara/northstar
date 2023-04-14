package org.dromara.northstar.main.handler.internal;

import java.util.EnumSet;
import java.util.Set;

import org.dromara.northstar.main.mail.MailDeliveryManager;

import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.GenericEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;

public class MailBindedEventHandler extends AbstractEventHandler implements GenericEventHandler{

	private MailDeliveryManager mailMgr;
	
	private static final Set<NorthstarEventType> TARGET_TYPE = EnumSet.of(
			NorthstarEventType.CONNECTED,
			NorthstarEventType.DISCONNECTED,
			NorthstarEventType.NOTICE,
			NorthstarEventType.TRADE,
			NorthstarEventType.ORDER
	); 
	
	public MailBindedEventHandler(MailDeliveryManager mailMgr) {
		this.mailMgr = mailMgr;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		mailMgr.onEvent(e);
	}

}
