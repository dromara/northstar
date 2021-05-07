package tech.xuanwu.northstar.engine.event.handler;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.PluginEventBus;
import tech.xuanwu.northstar.engine.event.EventEngine.NorthstarEventHandler;

public class PluginHandler implements NorthstarEventHandler {

	private PluginEventBus eb;
	
	public PluginHandler(PluginEventBus eb){
		this.eb = eb;
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		eb.post(event);
	}

}
