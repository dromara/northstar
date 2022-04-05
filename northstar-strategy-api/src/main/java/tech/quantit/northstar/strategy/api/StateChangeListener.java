package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.common.constant.ModuleState;

public interface StateChangeListener {

	void onChange(ModuleState state);
}
