package org.dromara.northstar.strategy.rl;

/**
 * 模型输入的预处理类型（DJL Translator 与 Python 训练侧共享的同一份策略）。
 *
 * 与 metadata.json 中的 preprocessing.type 取值一一对应（小写形式）。
 */
public enum PreprocessType {

	/** 直通：策略已保证特征归一化，无需额外预处理 */
	IDENTITY,
	/** 标准化：x' = (x - mean) / std，统计量在 v1 首次训练时计算并冻结，fork 版本默认沿用 */
	STANDARDIZE;

	/** metadata.json 中的序列化形式（小写） */
	public String code() {
		return name().toLowerCase();
	}

	public static PreprocessType of(String code) {
		for (PreprocessType t : values()) {
			if (t.code().equals(code)) {
				return t;
			}
		}
		throw new IllegalArgumentException("未知的预处理类型: " + code);
	}
}
