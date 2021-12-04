package tech.quantit.northstar.common.event;

public interface GenericEventHandler {

	void onEvent(NorthstarEvent e);
	
	boolean canHandle(NorthstarEventType eventType);
}
