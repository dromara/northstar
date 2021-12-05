package tech.quantit.northstar.strategy.api;

import com.google.common.eventbus.Subscribe;

import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;

public interface EventDrivenComponent extends Subscribable {

	@Subscribe
	void onEvent(ModuleEvent<?> moduleEvent);
	
	void setEventBus(ModuleEventBus moduleEventBus);
}
