package tech.quantit.northstar.main.engine.event.handler;

import java.util.HashSet;
import java.util.Set;

import tech.quantit.northstar.common.event.MarketDataEventBus;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;

public class MarketDataDispatcher implements NorthstarEventDispatcher {
	
	private MarketDataEventBus mdeb;
	
	private Set<NorthstarEventType> canHandleEvents = new HashSet<>() {
		private static final long serialVersionUID = 1L;

		{
			add(NorthstarEventType.TICK);
			add(NorthstarEventType.IDX_TICK);
			add(NorthstarEventType.CONTRACT_LOADED);
		}
	};
	
	public MarketDataDispatcher(MarketDataEventBus mdeb) {
		this.mdeb = mdeb;
	}

	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(canHandleEvents.contains(event.getEvent())) {
			mdeb.post(event);
		}
	}

}
