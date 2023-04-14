package org.dromara.northstar.main.engine.event.handler;

import org.dromara.northstar.common.event.InternalEventBus;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;

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
