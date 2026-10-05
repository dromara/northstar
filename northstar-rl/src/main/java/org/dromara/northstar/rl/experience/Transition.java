package org.dromara.northstar.rl.experience;

/**
 * 一条转移样本（决策点粒度，事件驱动决策 / SMDP 语义）。
 *
 * @param state			决策点状态向量
 * @param action		动作下标（= 动作枚举 ordinal）
 * @param reward		决策区间奖励（区间累计收益 - 交易成本）
 * @param nextState		下一决策点状态向量
 * @param dt			决策间隔 bar 数（训练侧按 γ^dt 折扣）
 * @param terminated	是否真失败终止（权益击穿）；数据耗尽的 truncated 记 false
 */
public record Transition(float[] state, int action, float reward, float[] nextState, int dt, boolean terminated) {
}
