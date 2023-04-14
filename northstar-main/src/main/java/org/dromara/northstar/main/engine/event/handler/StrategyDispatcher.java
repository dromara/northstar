package org.dromara.northstar.main.engine.event.handler;

import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.StrategyEventBus;
import org.dromara.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;

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
