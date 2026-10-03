package org.dromara.northstar.strategy.rl;

/**
 * RL 状态特征定义。
 *
 * @param name			特征名（specHash 计算与结构化 diff 的标识）
 * @param normalizer	归一化类型
 * @param description	特征说明（可选）
 */
public record RLFeatureSpec(String name, NormalizerType normalizer, String description) {

	public RLFeatureSpec(String name, NormalizerType normalizer) {
		this(name, normalizer, "");
	}
}
