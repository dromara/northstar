package tech.xuanwu.northstar.engine.event.handler;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.engine.event.FastEventEngine.NorthstarEventHandler;

public class InternalHandler implements NorthstarEventHandler {

	private InternalEventBus eb;
	
	public InternalHandler(InternalEventBus eb){
		this.eb = eb;
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		eb.post(event);
	}

}
