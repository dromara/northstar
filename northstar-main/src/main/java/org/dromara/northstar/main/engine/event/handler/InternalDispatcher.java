package org.dromara.northstar.main.engine.event.handler;

import tech.quantit.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.common.event.NorthstarEvent;

public class InternalDispatcher implements NorthstarEventDispatcher {

	private InternalEventBus eb;
	
	public InternalDispatcher(InternalEventBus eb){
		this.eb = eb;
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		eb.post(event);
	}

}
