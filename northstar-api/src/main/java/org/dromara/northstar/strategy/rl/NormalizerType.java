package org.dromara.northstar.strategy.rl;

/**
 * 状态特征的归一化类型。
 *
 * 强化学习的状态特征必须是与合约量纲无关的归一化/比例量（收益率、相对均线偏离、盈亏比等），
 * 该枚举用于声明特征的归一化方式，是模型规格（specHash）的一部分。
 */
public enum NormalizerType {

	/** 原始量（仅在特征本身无量纲时使用，例如 0/1 持仓标记） */
	NONE,
	/** 比例量：收益率、相对偏离等 (a - b) / b 形式 */
	RATIO,
	/** 标准化量：(x - mean) / std，统计量由模型版本 metadata 冻结 */
	Z_SCORE
}
