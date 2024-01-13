package org.dromara.northstar.ai.rl.model;

/**
 * 强化学习马可夫过程中的经验元组
 * @auth KevinHuangwl
 */
public record RLExperience(
		/**
		 * 当前状态
		 */
		RLState state,
		/**
		 * 对应采取的行动
		 */
		RLAction action,
		/**
		 * 行动对应的奖励
		 */
		RLReward reward,
		/**
		 * 行动后的状态
		 */
		RLState nextState,
		/**
		 * 回合是否结束
		 */
		boolean terminated
	) 
{}
