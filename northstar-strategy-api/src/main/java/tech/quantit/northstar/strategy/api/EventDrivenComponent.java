package tech.quantit.northstar.strategy.api;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import tech.quantit.northstar.common.Subscribable;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;

@Deprecated
public interface EventDrivenComponent extends Subscribable {

	@Subscribe
	void onEvent(ModuleEvent<?> moduleEvent);
	
	void setEventBus(EventBus moduleEventBus);
}
