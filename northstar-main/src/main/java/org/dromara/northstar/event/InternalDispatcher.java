package org.dromara.northstar.event;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.dromara.northstar.common.event.NorthstarEvent;

@Component
public class InternalDispatcher implements NorthstarEventDispatcher {

	@Autowired
	private AccountHandler accountHandler;
	@Autowired
	private ConnectionHandler connHandler;
	@Autowired
	private MailBindedEventHandler mailHandler;
	@Autowired
	private MarketDataHandler mdHandler;
	@Autowired
	private ModuleManager moduleMgr;
	@Autowired
	private SimMarketHandler simMarketHandler;
	
	public InternalDispatcher(FastEventEngine feEngine) {
		feEngine.addHandler(this);
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		moduleMgr.onEvent(event);
		simMarketHandler.onEvent(event);
		accountHandler.onEvent(event);
		connHandler.onEvent(event);
		mdHandler.onEvent(event);
		mailHandler.onEvent(event);
		
	}

}
