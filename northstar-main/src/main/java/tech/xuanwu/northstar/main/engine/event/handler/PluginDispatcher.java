package tech.xuanwu.northstar.main.engine.event.handler;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.PluginEventBus;
import tech.xuanwu.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;

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
