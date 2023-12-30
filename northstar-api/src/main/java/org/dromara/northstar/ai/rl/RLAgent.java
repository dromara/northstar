package org.dromara.northstar.ai.rl;

import java.io.Closeable;

import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.ai.rl.model.RLExperience;
import org.dromara.northstar.ai.rl.model.RLState;

/**
 * 强化学习中的智能体
 * @auth KevinHuangwl
 */
public interface RLAgent extends Closeable{
	
	/**
	 * 响应环境状态
	 * @param state
	 * @param reward
	 * @return
	 */
	RLAction react(RLState state);
	
	/**
	 * 学习经验
	 * @param exp
	 */
	void learn(RLExperience exp);
	
	/**
	 * 更新经验，对于off-policy的模型有用
	 */
	void update();
	
	/**
	 * 保存模型
	 * @param name
	 */
	void save(String name);
	
	/**
	 * 加载模型
	 * @param name
	 */
	void load(String name);
}
