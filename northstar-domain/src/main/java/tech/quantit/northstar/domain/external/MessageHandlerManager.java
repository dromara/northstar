package tech.quantit.northstar.domain.external;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import tech.quantit.northstar.common.IMessageHandlerManager;
import tech.quantit.northstar.common.MessageHandler;

public class MessageHandlerManager implements IMessageHandlerManager{

	private Set<MessageHandler> handlerSet = new HashSet<>();
	
	@Override
	public void addHandler(MessageHandler handler) {
		handlerSet.add(handler);
	}

	@Override
	public Collection<MessageHandler> getHandlers() {
		return handlerSet;
	}

}
