package org.dromara.northstar.ai.rl;

import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.ai.rl.model.RLReward;
import org.dromara.northstar.ai.rl.model.RLState;

/**
 * 强化学习中的智能体
 * @auth KevinHuangwl
 */
public interface RLAgent {
	
	RLAction react(RLState state, RLReward reward);
}
