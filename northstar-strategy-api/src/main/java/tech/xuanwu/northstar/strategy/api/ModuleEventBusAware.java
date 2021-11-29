package tech.xuanwu.northstar.strategy.api;

import tech.xuanwu.northstar.strategy.api.event.ModuleEventBus;

public interface ModuleEventBusAware {

	void setEventBus(ModuleEventBus moduleEventBus);
}
