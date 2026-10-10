package org.dromara.northstar.strategy.rl;

/**
 * RL 训练算法标识。
 *
 * 由策略通过 {@link RLStrategy#rlAlgo()} 声明，写入模型级 metadata.json 的 algo 字段，
 * Java 训练服务（超参数注册表 {@code RLAlgorithms}）、Java 推理侧（{@code RLAgent}）
 * 与 Python 训练侧（train_round.py）均据其分发到具体算法实现。
 *
 * v1 仅 DQN 有完整实现；PPO 为 v2 预留，涉及处显式快速失败。
 */
public enum RLAlgo {

	DQN,

	/** v2 预留：训练侧与推理侧暂未实现，触碰即快速失败 */
	PPO
}
