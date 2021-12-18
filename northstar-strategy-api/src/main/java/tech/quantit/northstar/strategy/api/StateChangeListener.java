package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.strategy.api.constant.ModuleState;

public interface StateChangeListener {

	void onChange(ModuleState state);
}
