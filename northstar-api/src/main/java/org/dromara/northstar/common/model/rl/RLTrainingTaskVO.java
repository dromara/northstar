package org.dromara.northstar.common.model.rl;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RL 训练任务视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RLTrainingTaskVO {

	private String taskId;
	private String strategyClass;
	private String strategyName;
	private String modelId;
	private String modelName;
	private int baseVersion;
	private int targetVersion;
	private List<String> contracts;
	private String warmupStartDate;
	private String trainStartDate;
	private String validationStartDate;
	private int totalRounds;
	private int episodesPerRound;
	private RLTrainingStatus status;
	private int currentRound;
	private long createdAt;
	/** 所选模型的算法（DQN/PPO），展示用 */
	private String algo;
	private Map<String, Double> hyperParams;
	/** 最新一轮的平均奖励 */
	private Double latestAvgReward;
	/** 最新一轮的 loss */
	private Double latestLoss;
	/** 失败原因（status=FAILED 时有值） */
	private String failReason;
}
