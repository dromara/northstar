package org.dromara.northstar.strategy.rl;

/**
 * 自定义预处理函数：观测向量进入网络前的逐样本变换（{@link PreprocessType#CUSTOM} 时使用）。
 *
 * 刻意收敛为 float[] → float[] 的输入侧契约，而非 DJL Translator 全量定制：
 * 输出侧固定为 Q 值 argmax，放开会让自定义代码破坏动作枚举契约。
 *
 * 实现契约（训推一致的前提）：
 * <ul>
 *   <li><b>无状态、确定性</b>：同一输入必须永远得到同一输出（不允许内部累积统计量，
 *       需要数据相关统计量请改用 {@link PreprocessType#STANDARDIZE}，由训练侧冻结）；</li>
 *   <li><b>保长</b>：输出维度必须等于输入维度（= 状态特征数）；</li>
 *   <li><b>不改入参</b>：不得修改传入数组，须返回新数组
 *       （训练主循环会复用原始观测，推理侧同一份原始观测可能被多次变换）。</li>
 * </ul>
 *
 * 调用位置：训练时 {@code TradingEnvironment} 在落盘 transition 前对每个状态向量调用
 * （Python 训练侧对 custom 直通）；推理时 {@code RLAgent} 在进入网络前调用。
 * 两侧必须是语义等价的变换，否则训练分布与推理分布不一致。
 */
@FunctionalInterface
public interface RLStatePreprocessor {

	/**
	 * @param rawState	策略 {@link RLStrategy#observe} 的原始输出（未做任何预处理）
	 * @return			网络输入空间的向量（新数组，长度与入参一致）
	 */
	float[] apply(float[] rawState);
}
