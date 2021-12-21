package tech.quantit.northstar.common;

import tech.quantit.northstar.common.event.NorthstarEvent;

public interface MessageHandler {

	void onEvent(NorthstarEvent event);
}
