package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.model.ModuleAgent;

public interface ModuleAware {

	/**
	 * 设置模组代理
	 * @param agent
	 */
	void setModuleAgent(ModuleAgent agent);
}
