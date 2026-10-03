package org.dromara.northstar.strategy.rl;

/**
 * 交易动作枚举：HOLD 观望 / BUY 做多 / SELL 做空（平仓或反手由策略的 onAction 翻译）。
 *
 * 枚举名称与顺序即模型的动作表：ordinal = 神经网络输出下标，
 * 与模型 metadata.json 的 actions 表逐一对应。
 */
public enum TradeAction {

	HOLD,
	BUY,
	SELL
}
