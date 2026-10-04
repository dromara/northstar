package org.dromara.northstar.common.model.rl;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建 RL 训练任务的请求体（策略驱动）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RLTrainingTaskDTO {

	/** RL 策略类全限定名（仅接受实现 RLStrategy 的策略） */
	private String strategyClass;
	/** 模型 ID（模型目录名，按所选策略过滤） */
	private String modelId;
	/** 基准版本：0 = 从零训练；否则为某历史版本号，决定超参数预填值与权重继承 */
	private int baseVersion;
	/** 采样合约（指数合约 unifiedSymbol 列表） */
	private List<String> contracts;
	/** 预热起始日（YYYY-MM-DD），预热范围 = [预热起始日, 训练起始日) */
	private String warmupStartDate;
	/** 训练起始日（YYYY-MM-DD），训练范围 = [训练起始日, 验证起始日) */
	private String trainStartDate;
	/** 验证起始日（YYYY-MM-DD），验证范围 = [验证起始日, 数据末日] */
	private String validationStartDate;
	/** 总轮数 */
	private int totalRounds;
	/** 每轮局数（全部合约合计，均分到各合约环境） */
	private int episodesPerRound;
	/** 沙箱每局初始资金（正数） */
	private Double initBalance;
	/** 决策点超时强触发 bar 数（防死行情里永不触发导致数据饿死，>=1） */
	private Integer maxHoldingBars;
	/** 权益击穿阈值（相对初始资金的最大回撤比例，(0,1)，击穿即 terminated） */
	private Double ruinThreshold;
	/** 单局最长 bar 数（采样预算，>=1） */
	private Integer maxEpisodeBars;
	/** 超参数取值表（字段清单由模型的 Hyperparameter 类定义） */
	private Map<String, Double> hyperParams;
	/** 显式降级路径：baseVersion > 0 时设为 false 表示不继承权重从零训练（血缘仍记录父版本） */
	private Boolean inheritWeights;
}
