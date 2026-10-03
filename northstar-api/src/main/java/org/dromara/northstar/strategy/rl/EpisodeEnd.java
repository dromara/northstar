package org.dromara.northstar.strategy.rl;

/**
 * 回合结束类型（决策区间的终止语义）。
 */
public enum EpisodeEnd {

	/** 非回合边界（普通决策点） */
	NONE,
	/** 数据耗尽导致的正常切完（截断），不是失败 */
	TRUNCATED,
	/** 权益击穿等导致的真失败，通常配重罚 */
	TERMINATED
}
