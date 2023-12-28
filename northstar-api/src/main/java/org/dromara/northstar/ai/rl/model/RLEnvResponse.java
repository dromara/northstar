package org.dromara.northstar.ai.rl.model;

/**
 * 马可夫过程中的环境响应
 * @auth KevinHuangwl
 */
public record RLEnvResponse(
		/**
		 * 状态
		 */
		RLState state, 
		/**
		 * 奖励
		 */
		RLReward reward, 
		/**
		 * 是否结束
		 */
		boolean hasDone) {

}
