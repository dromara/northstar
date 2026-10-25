package org.dromara.northstar.app.rl;

import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.model.rl.RLRoundMetrics;
import org.dromara.northstar.common.model.rl.RLTrainingStatus;
import org.dromara.northstar.strategy.rl.RLAlgo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RL 训练任务的持久化状态（RL_TRAINING_TASK 表 dataStr 的 JSON 载荷）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RLTrainingTaskRecord {

	private String taskId;
	private String strategyClass;
	private String strategyName;
	private String modelId;
	private String modelName;
	private RLAlgo algo;
	private int baseVersion;
	private int targetVersion;
	private List<String> contracts;
	private String warmupStartDate;
	private String trainStartDate;
	private String validationStartDate;
	private int totalRounds;
	private int episodesPerRound;
	/** 沙箱每局初始资金 */
	private double initBalance;
	/** 决策点超时强触发 bar 数 */
	private int maxHoldingBars;
	/** 权益击穿阈值（相对初始资金的最大回撤比例） */
	private double ruinThreshold;
	/** 单局最长 bar 数（采样预算） */
	private int maxEpisodeBars;
	/** 默认开仓数量（正数，允许小数） */
	private double defaultVolume;
	private Map<String, Double> hyperParams;
	private RLTrainingStatus status;
	private int currentRound;
	/** 数据加载进度：已就绪合约数（含跳过） */
	private int dataLoadedContracts;
	/** 数据加载进度：累计 bar 数 */
	private long dataLoadedBars;
	/** 数据加载进度：正在加载的合约 */
	private String dataCurrentContract;
	private long createdAt;
	/** 采样随机种子（记入版本 metadata，同一 seed 可完全复现采样序列） */
	private long seed;
	private String failReason;
	private List<RLRoundMetrics> metrics;

	/** 日志展示名：模型名+基线→目标版本（如 "rl-trend v1→v2"），比随机 taskId 更可读，便于日志排查定位 */
	public String displayName() {
		return "%s v%d→v%d".formatted(modelName, baseVersion, targetVersion);
	}
}
