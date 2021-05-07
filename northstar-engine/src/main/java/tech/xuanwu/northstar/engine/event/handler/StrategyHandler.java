package tech.xuanwu.northstar.engine.event.handler;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.StrategyEventBus;
import tech.xuanwu.northstar.engine.event.EventEngine.NorthstarEventHandler;

public class StrategyHandler implements NorthstarEventHandler {

	private StrategyEventBus eb;
	
	public StrategyHandler(StrategyEventBus eb){
		this.eb = eb;
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		eb.post(event);
	}

}
