package org.dromara.northstar.strategy.ai;

public interface RLAware {
	
	/**
	 * 提供奖励函数
	 * @return
	 */
	Reward reward();
	
	/**
	 * 提供状态信息 
	 * @return
	 */
	State state();
	
	/**
	 * 响应操作
	 * @param action
	 */
	void onAction(Action action);
	
	/**
	 * 获取环境对象
	 * @return
	 */
	AiEnvironment getAiEnvironment();
}
