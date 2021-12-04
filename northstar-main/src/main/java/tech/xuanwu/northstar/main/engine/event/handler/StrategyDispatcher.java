package tech.xuanwu.northstar.main.engine.event.handler;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.StrategyEventBus;
import tech.xuanwu.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;

public class StrategyDispatcher implements NorthstarEventDispatcher {

	private StrategyEventBus eb;
	
	public StrategyDispatcher(StrategyEventBus eb){
		this.eb = eb;
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		eb.post(event);
	}

}
