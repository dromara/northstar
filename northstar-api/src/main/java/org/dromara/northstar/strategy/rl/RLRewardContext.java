package org.dromara.northstar.strategy.rl;

import java.util.List;

import org.dromara.northstar.common.model.core.Trade;

/**
 * 环境在每个决策点构造的"结算单"，供策略的 reward() 做纯函数计算。
 *
 * 事件驱动语义下，奖励按决策区间而非逐 bar 计算。
 *
 * @param prevEquity	上一个决策点的模组权益
 * @param currEquity	当前决策点的模组权益（区间收益主成分 = currEquity - prevEquity）
 * @param trades		决策区间内的成交记录（供显式扣除交易成本）
 * @param dt			决策间隔 bar 数（奖励整形参考；训练侧按 γ^dt 折扣）
 * @param episodeEnd	回合结束类型（区分 truncated 与 terminated；止损只是普通决策点，即 NONE）
 */
public record RLRewardContext(
		double prevEquity,
		double currEquity,
		List<Trade> trades,
		int dt,
		EpisodeEnd episodeEnd) {

	/** 区间收益 = currEquity - prevEquity */
	public double intervalProfit() {
		return currEquity - prevEquity;
	}

	/** 区间成交金额合计（供策略估算交易成本；精确手续费已体现在模组权益账本中） */
	public double turnover() {
		return trades.stream()
				.mapToDouble(t -> t.price() * t.volume() * t.contract().multiplier())
				.sum();
	}
}
