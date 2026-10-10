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
	STANDARDIZE,
	/**
	 * 自定义：变换由策略通过 {@link RLStrategy#rlPreprocessor()} 提供。
	 * 训练时 Java 侧在落盘 transition 前完成变换（Python 直通，不冻结统计量）；
	 * 推理时 Java 侧在进入网络前完成变换；两侧必须使用同一份无状态、确定性的变换。
	 */
	CUSTOM;

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
