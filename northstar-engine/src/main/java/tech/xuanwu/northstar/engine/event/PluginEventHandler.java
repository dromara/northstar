package tech.xuanwu.northstar.engine.event;

import org.springframework.beans.factory.InitializingBean;

import com.google.common.eventbus.EventBus;

import tech.xuanwu.northstar.engine.event.EventEngine.Event;
import tech.xuanwu.northstar.engine.event.EventEngine.NorthstarEventHandler;

public class PluginEventHandler implements NorthstarEventHandler, InitializingBean {

	private EventEngine ee;
	
	private EventBus eb;
	
	public PluginEventHandler(EventEngine ee, EventBus eb){
		this.ee = ee;
		this.eb = eb;
	}
	
	@Override
	public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
		eb.post(event);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ee.addHandler(this);
	}

}
