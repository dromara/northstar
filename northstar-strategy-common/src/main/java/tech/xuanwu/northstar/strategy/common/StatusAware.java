package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;

public interface StatusAware {

	/**
	 * 设置模组状态
	 * @param stateMachine
	 */
	void setModuleStatus(ModuleStatus status);
}
