package org.dromara.northstar.ai.rl;

import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.ai.rl.model.RLReward;
import org.dromara.northstar.ai.rl.model.RLState;
import org.dromara.northstar.common.model.Tuple;

/**
 * 强化学习马可夫过程中的环境
 * @auth KevinHuangwl
 */
public interface RLEnvironment {

	Tuple<RLState, RLReward> interact(RLAction action);
	
}
