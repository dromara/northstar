package org.dromara.northstar.common.model.rl;

/**
 * RL 训练任务状态。
 */
public enum RLTrainingStatus {

	/** 已创建，待加载数据 */
	CREATED,
	/** 数据加载中（从数据源拉取并落地 CSV） */
	LOADING,
	/** 数据就绪，可开始训练 */
	READY,
	RUNNING,
	PAUSED,
	FINISHED,
	FAILED
}
