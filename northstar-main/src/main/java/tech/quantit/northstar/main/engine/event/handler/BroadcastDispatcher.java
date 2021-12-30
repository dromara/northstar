package tech.quantit.northstar.main.engine.event.handler;

import java.util.HashSet;
import java.util.Set;

import tech.quantit.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.main.handler.broadcast.SocketIOMessageEngine;

public class BroadcastDispatcher implements NorthstarEventDispatcher {
	
	private SocketIOMessageEngine msgEngine;
	
	private static Set<NorthstarEventType> eventSet = new HashSet<>() {
		private static final long serialVersionUID = 1L;
		{
			add(NorthstarEventType.TICK);
			add(NorthstarEventType.BAR);
			add(NorthstarEventType.ACCOUNT);
			add(NorthstarEventType.ORDER);
			add(NorthstarEventType.POSITION);
			add(NorthstarEventType.TRADE);
			add(NorthstarEventType.NOTICE);
		}
	};
	
	public BroadcastDispatcher(SocketIOMessageEngine msgEngine) {
		this.msgEngine = msgEngine;
	}

	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(!eventSet.contains(event.getEvent())) {
			return;
		}
		msgEngine.emitEvent(event);
	}

}
