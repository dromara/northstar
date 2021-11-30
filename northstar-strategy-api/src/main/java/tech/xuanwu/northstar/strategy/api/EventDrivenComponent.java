package tech.xuanwu.northstar.strategy.api;

import com.google.common.eventbus.Subscribe;

import tech.xuanwu.northstar.strategy.api.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventBus;

public interface EventDrivenComponent {

	@Subscribe
	void onEvent(ModuleEvent<?> moduleEvent);
	
	void setEventBus(ModuleEventBus moduleEventBus);
}
