package tech.xuanwu.northstar.engine.event.handler;

import com.google.common.eventbus.EventBus;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.engine.event.EventEngine.NorthstarEventHandler;

public class StrategyHandler implements NorthstarEventHandler {

	private EventBus eb;
	
	public StrategyHandler(EventBus eb){
		this.eb = eb;
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		eb.post(event);
	}

}
