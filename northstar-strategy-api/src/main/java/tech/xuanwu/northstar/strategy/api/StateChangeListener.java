package tech.xuanwu.northstar.strategy.api;

import tech.xuanwu.northstar.strategy.api.constant.ModuleState;

public interface StateChangeListener {

	void onChange(ModuleState state);
}
