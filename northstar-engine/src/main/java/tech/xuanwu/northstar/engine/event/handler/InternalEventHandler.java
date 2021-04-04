package tech.xuanwu.northstar.engine.event.handler;

import org.springframework.beans.factory.InitializingBean;

import com.google.common.eventbus.EventBus;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.engine.event.EventEngine;
import tech.xuanwu.northstar.engine.event.EventEngine.NorthstarEventHandler;

public class InternalEventHandler implements NorthstarEventHandler, InitializingBean {

	private EventEngine ee;
	
	private EventBus eb;
	
	public InternalEventHandler(EventEngine ee, EventBus eb){
		this.ee = ee;
		this.eb = eb;
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		eb.post(event);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ee.addHandler(this);
	}
}
