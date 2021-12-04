package tech.quantit.northstar.main.engine.event.handler;

import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.PluginEventBus;
import tech.quantit.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;

public class PluginDispatcher implements NorthstarEventDispatcher {

	private PluginEventBus eb;
	
	public PluginDispatcher(PluginEventBus eb){
		this.eb = eb;
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		eb.post(event);
	}

}
