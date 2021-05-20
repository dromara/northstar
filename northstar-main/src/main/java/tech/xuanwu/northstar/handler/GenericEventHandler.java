package tech.xuanwu.northstar.handler;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;

public interface GenericEventHandler {

	void onEvent(NorthstarEvent e);
	
	boolean canHandle(NorthstarEventType eventType);
}
