package tech.xuanwu.northstar.strategy.api;

import tech.xuanwu.northstar.strategy.api.event.ModuleEvent;

public interface EventDrivenComponent {

	void onEvent(ModuleEvent<?> moduleEvent);
}
