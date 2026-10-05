package org.dromara.northstar.rl.training;

import java.util.LinkedHashMap;
import java.util.Map;

import org.dromara.northstar.common.model.DynamicParams;

/**
 * 训练算法注册表：algo -> Hyperparameter 类。
 *
 * 算法差异（如 DQN 有 epsilon 三项、PPO 有裁剪率/熵系数）由各自的 Hyperparameter 类天然表达，
 * 前端零特判。v1 只支持 DQN；PPO 的训练侧实现（ppo/ 包与 on-policy ExperienceStore）延后到 v2。
 */
public final class RLAlgorithms {

	public static final String DQN = "DQN";
	public static final String PPO = "PPO";

	private RLAlgorithms() {
	}

	/** 该算法的超参数定义（字段清单随实例的默认值一起返回） */
	public static DynamicParams hyperParamsOf(String algo) {
		return switch (algo) {
			case DQN -> new DqnHyperParams();
			case PPO -> throw new UnsupportedOperationException("PPO 训练侧将在 v2 提供");
			default -> throw new IllegalArgumentException("未知的训练算法: " + algo);
		};
	}

	/** 该算法的超参数默认值表（字段名 -> 默认值），与 Python 训练侧共享同一套键（camelCase） */
	public static Map<String, Double> hyperParamDefaults(String algo) {
		Map<String, Double> defaults = new LinkedHashMap<>();
		hyperParamsOf(algo).getMetaInfo().forEach(field ->
				defaults.put(field.getName(), Double.parseDouble(String.valueOf(field.getValue()))));
		return defaults;
	}
}
