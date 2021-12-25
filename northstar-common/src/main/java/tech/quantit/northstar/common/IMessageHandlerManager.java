package tech.quantit.northstar.common;

import java.util.Collection;

public interface IMessageHandlerManager {

	void addHandler(MessageHandler handler);
	
	Collection<MessageHandler> getHandlers();
}
