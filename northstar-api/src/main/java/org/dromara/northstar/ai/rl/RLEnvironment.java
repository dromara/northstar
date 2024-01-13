package org.dromara.northstar.ai.rl;

import java.io.Closeable;
import java.io.IOException;

import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.ai.rl.model.RLEnvResponse;
import org.dromara.northstar.ai.rl.model.RLState;

/**
 * 强化学习马可夫过程中的环境
 * @auth KevinHuangwl
 */
public interface RLEnvironment extends Closeable{

	/**
	 * 与环境进行交互
	 * @param action
	 * @return
	 */
	RLEnvResponse interact(RLAction action) throws IOException;
	/**
	 * 重置环境
	 * @return
	 */
	RLState reset() throws IOException;
}
