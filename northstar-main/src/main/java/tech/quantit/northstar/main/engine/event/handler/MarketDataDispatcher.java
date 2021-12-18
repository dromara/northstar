package tech.quantit.northstar.main.engine.event.handler;

import tech.quantit.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.main.handler.data.MarketBarDataPersistenceHandler;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public class MarketDataDispatcher implements NorthstarEventDispatcher {
	
	private MarketBarDataPersistenceHandler persistenceHandler;
	
	public MarketDataDispatcher(MarketBarDataPersistenceHandler persistenceHandler) {
		this.persistenceHandler = persistenceHandler;
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(event.getData() instanceof TickField tick) {
			persistenceHandler.onTick(tick);
		} else if(event.getData() instanceof BarField bar) {
			persistenceHandler.onBar(bar);
		}
	}

}
