package tech.quantit.northstar.main.engine.event.handler;

import tech.quantit.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.event.PlaybackEventBus;

public class PlaybackDispatcher implements NorthstarEventDispatcher{
	
	private PlaybackEventBus eventBus;
	
	public PlaybackDispatcher(PlaybackEventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(event.getEvent() != NorthstarEventType.TICK && event.getEvent() != NorthstarEventType.BAR) {
			eventBus.post(event);
		}
	}

}
