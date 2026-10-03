package org.dromara.northstar.common.model.rl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每轮训练指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RLRoundMetrics {

	private int round;
	/** 平均奖励（每条 transition） */
	private double avgReward;
	/** 本轮累计回报 */
	private double cumulativeReward;
	/** 训练 loss（Python stdout 结构化输出） */
	private double loss;
	private double epsilon;
	private double lr;
	/** 平均决策间隔（bar 数/决策，观测触发条件松紧） */
	private double avgDecisionInterval;
	/** 验证集胜率（每 N 轮评估一次） */
	private Double evalWinRate;
	/** 验证集盈亏比 */
	private Double evalProfitFactor;
	/** 验证集交易笔数 */
	private Integer evalTrades;
	/** 验证集收益率（%） */
	private Double evalReturn;
}
