package org.dromara.northstar.strategy.rl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RL 模型规格：由策略代码定义的模型"形状"（状态特征清单 + 动作枚举 + 预处理类型）。
 *
 * 该规格创建模型后终身不可变；specHash 是对规格的稳定性指纹，
 * 在创建训练任务、fork 版本、模组加载推理、单元测试四个时机快速失败校验。
 *
 * @param features		状态特征清单（顺序敏感，顺序即状态向量下标；stateDim = features.size()）
 * @param actionEnum	动作枚举类（枚举名称与顺序即动作表；actionDim = 枚举常量数）
 * @param preprocessing	输入预处理类型
 */
public record RLModelSpec(
		List<RLFeatureSpec> features,
		Class<? extends Enum<?>> actionEnum,
		PreprocessType preprocessing) {

	public RLModelSpec {
		features = List.copyOf(features);
		if (features.isEmpty()) {
			throw new IllegalArgumentException("状态特征清单不能为空");
		}
		if (actionEnum == null || actionEnum.getEnumConstants().length == 0) {
			throw new IllegalArgumentException("动作枚举不能为空");
		}
		if (preprocessing == null) {
			throw new IllegalArgumentException("预处理类型不能为空");
		}
	}

	public int stateDim() {
		return features.size();
	}

	public int actionDim() {
		return actionEnum.getEnumConstants().length;
	}

	/** 动作名表：actions[i] = 动作下标 i 对应的名称，与枚举 ordinal 一一对应 */
	public List<String> actions() {
		return Arrays.stream(actionEnum.getEnumConstants()).map(Enum::name).toList();
	}

	/**
	 * 规格指纹：对 features(name+normalizer，顺序敏感) + actions + preprocessing 稳定序列化后
	 * 取 SHA-256 前 16 位。
	 */
	public String specHash() {
		StringBuilder sb = new StringBuilder();
		for (RLFeatureSpec f : features) {
			sb.append(f.name()).append(':').append(f.normalizer()).append(';');
		}
		sb.append('|');
		for (String a : actions()) {
			sb.append(a).append(';');
		}
		sb.append('|').append(preprocessing);
		return sha256Hex16(sb.toString());
	}

	/**
	 * 与另一份规格做结构化比对（用于策略演进防护：输出哪个特征变了/新增了什么，而不是只报 hash 不一致）。
	 *
	 * @return 差异描述列表；为空表示两份规格等价
	 */
	public List<String> diff(RLModelSpec other) {
		List<String> diffs = new ArrayList<>();
		int minLen = Math.min(features.size(), other.features.size());
		for (int i = 0; i < minLen; i++) {
			RLFeatureSpec a = features.get(i);
			RLFeatureSpec b = other.features.get(i);
			if (!a.name().equals(b.name()) || a.normalizer() != b.normalizer()) {
				diffs.add(String.format("特征[%d]变更: %s(%s) -> %s(%s)", i, a.name(), a.normalizer(), b.name(), b.normalizer()));
			}
		}
		for (int i = minLen; i < features.size(); i++) {
			diffs.add(String.format("特征[%d]被移除: %s", i, features.get(i).name()));
		}
		for (int i = minLen; i < other.features.size(); i++) {
			diffs.add(String.format("特征[%d]新增: %s", i, other.features.get(i).name()));
		}
		if (!actions().equals(other.actions())) {
			diffs.add(String.format("动作表变更: %s -> %s", actions(), other.actions()));
		}
		if (preprocessing != other.preprocessing) {
			diffs.add(String.format("预处理变更: %s -> %s", preprocessing, other.preprocessing));
		}
		return diffs;
	}

	private static String sha256Hex16(String content) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (int i = 0; i < 8; i++) {	// 前 8 字节 = 16 个十六进制字符
				hex.append(String.format("%02x", hash[i]));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
