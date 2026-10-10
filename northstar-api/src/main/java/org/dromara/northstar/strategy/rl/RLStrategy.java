package org.dromara.northstar.strategy.rl;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.strategy.IModuleStrategyContext;
import org.dromara.northstar.strategy.TradeStrategy;

/**
 * 可训练的 RL 策略接口：只有实现了该接口的策略才能被强化学习训练。
 *
 * 策略代码定义模型的"形状"（{@link #rlSpec()}）与三个训练/实盘共用钩子，
 * 保证训推一致：训练环境与实盘模组执行完全相同的代码路径。
 *
 * 探索（ε-greedy）与经验簿记（transition 记录）归训练环境，策略零 RL 簿记负担；
 * RL 动作最终翻译成普通下单调用，既有风控过滤器、状态机、资金校验全部继续生效。
 */
public interface RLStrategy extends TradeStrategy {

	/**
	 * 模型名：即网络结构身份，与 Python 训练侧 registry.py 中同名注册的网络构建函数一一对应。
	 * 模型目录名（models/{modelName}/）与模型级 metadata.json 的 name 字段均取自该值。
	 */
	String modelName();

	/**
	 * 训练算法：声明本模型用哪种算法训练。
	 *
	 * 与 {@link #modelName()} 正交：网络结构身份看 modelName，训练算法看本方法
	 * （同一网络主干可被不同算法复用，如 PPO 的 actor-critic 可共享 DQN 的 MLP）。
	 * 声明值写入模型级 metadata.json 的 algo 字段，Java/Python 训练侧均据其分发。
	 *
	 * 模型创建后不得变更：checkpoint 结构、超参历史与经验回放语义均按算法解释，
	 * 变更算法须换 modelName 建新模型（syncModel 检出声明变更即快速失败）。
	 */
	default RLAlgo rlAlgo() {
		return RLAlgo.DQN;
	}

	/**
	 * 模型规格：状态特征清单 + 动作枚举 + 预处理类型（创建模型后终身不可变）
	 */
	RLModelSpec rlSpec();

	/**
	 * 自定义预处理函数：仅当 {@code rlSpec().preprocessing() == PreprocessType.CUSTOM} 时
	 * 被框架调用（训练落盘前与推理进网络前各调用一次，两处必须是同一份无状态、确定性、保长的变换）。
	 *
	 * 默认实现快速失败：声明了 CUSTOM 却不提供预处理器会在创建训练任务时（syncModel 校验）暴露。
	 */
	default RLStatePreprocessor rlPreprocessor() {
		throw new UnsupportedOperationException(
				"rlSpec().preprocessing() 声明为 CUSTOM 时必须实现 rlPreprocessor()");
	}

	/**
	 * 状态向量构建：返回与 rlSpec().features 一一对应的归一化特征值
	 *
	 * @param ctx		模组上下文
	 * @param contract	采样合约
	 * @return			状态向量（长度 = stateDim）
	 */
	float[] observe(IModuleStrategyContext ctx, Contract contract);

	/**
	 * 动作 -> 下单翻译。训练与实盘共用同一份逻辑，保证训推一致。
	 * 实现内部应调用 ctx.submitOrderReq(...) 完成下单。
	 *
	 * @param action	模型输出动作
	 * @param ctx		模组上下文
	 * @param contract	采样合约
	 */
	void onAction(TradeAction action, IModuleStrategyContext ctx, Contract contract);

	/**
	 * 决策点触发条件：每根 bar 结束后由环境评估。
	 * 必须廉价：纯指标判断，不做模型推理。
	 *
	 * @return true 表示需要重新观测并决策
	 */
	boolean shouldObserve(IModuleStrategyContext ctx, Contract contract);

	/**
	 * 奖励函数：纯函数计算（不碰游标、不记账），含手续费/成本扣除。
	 *
	 * @param ctx	环境在决策点构造的结算单
	 * @return		该决策区间的奖励值
	 */
	double reward(RLRewardContext ctx);

	/**
	 * 指标预热所需的 bar 数：max(策略各指标回看周期) × 安全系数。
	 * 与实盘模组的 weeksOfDataForPreparation 语义对齐；
	 * 创建训练任务时会静态校验预热时段 bar 数不低于该值。
	 */
	default int rlWarmupBars() {
		return 60;
	}
}
