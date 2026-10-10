package org.dromara.northstar.app.rl;

import java.util.List;
import java.util.Random;

import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.rl.agent.RLAgent;
import org.dromara.northstar.rl.env.AdvanceResult;
import org.dromara.northstar.rl.env.MarketReplay;
import org.dromara.northstar.rl.experience.ExperienceStore;
import org.dromara.northstar.rl.experience.Transition;
import org.dromara.northstar.strategy.IModuleStrategyContext;
import org.dromara.northstar.strategy.rl.EpisodeEnd;
import org.dromara.northstar.strategy.rl.PreprocessType;
import org.dromara.northstar.strategy.rl.RLRewardContext;
import org.dromara.northstar.strategy.rl.RLStatePreprocessor;
import org.dromara.northstar.strategy.rl.RLStrategy;
import org.dromara.northstar.strategy.rl.TradeAction;

import lombok.extern.slf4j.Slf4j;

/**
 * 交易环境：一个"单合约世界"，gym 语义（reset/step/done）天然属于单环境。
 *
 * 每个环境组合一对 MarketReplay × RLModuleSandbox，并绑定一个 RLAgent 实例
 * （DJL Predictor 非线程安全，串行采样下任意时刻只有一个环境活跃）。
 *
 * 事件驱动决策（SMDP）：决策不按固定 bar 频率触发，由策略的 shouldObserve() 定义
 * "何时需要重新观测"；transition 只记录在决策点，配 maxHoldingBars 超时强触发兜底。
 */
@Slf4j
public class TradingEnvironment {

	/**
	 * @param initBalance		每局初始资金
	 * @param warmupBars		指标预热 bar 数（max(指标回看周期) × 安全系数）
	 * @param maxHoldingBars	决策点超时强触发（防死行情里永不触发导致数据饿死）
	 * @param ruinThreshold		权益击穿阈值（相对初始资金的最大回撤比例，击穿为 terminated 真失败）
	 * @param maxEpisodeBars	单局最长 bar 数（采样预算）
	 */
	public record RLEnvConfig(double initBalance, int warmupBars, int maxHoldingBars, double ruinThreshold,
			int maxEpisodeBars) {
	}

	/** 单轮采样统计 */
	public record SampleStats(double cumulativeReward, int transitions, long totalDt, int episodes) {
	}

	/** 验证时段评估产出（贪心策略） */
	public record EvalOutcome(List<DealRecord> deals, double initBalance, double finalEquity) {
	}

	private final Contract contract;
	private final MarketReplay replay;
	private final int trainStartIdx;
	private final int validationStartIdx;
	private final RLStrategy strategyTemplate;
	private final RLAgent agent;
	/** CUSTOM 预处理器：落盘 transition 前把原始观测变换到网络输入空间；null = identity/standardize（由 Python/推理侧处理） */
	private final RLStatePreprocessor preprocessor;
	private final int contractIdx;
	private final String moduleNamePrefix;
	private final IDataSource dataSource;
	private final RLEnvConfig config;

	public TradingEnvironment(IContract ic, List<Bar> ascendingBars,
			int trainStartIdx, int validationStartIdx, RLStrategy strategyTemplate, RLAgent agent, int contractIdx,
			String moduleNamePrefix, RLEnvConfig config) {
		this.contract = ic.contract();
		this.replay = new MarketReplay(ascendingBars);
		this.trainStartIdx = trainStartIdx;
		this.validationStartIdx = validationStartIdx;
		this.strategyTemplate = strategyTemplate;
		this.agent = agent;
		this.preprocessor = strategyTemplate.rlSpec().preprocessing() == PreprocessType.CUSTOM
				? strategyTemplate.rlPreprocessor()
				: null;
		this.contractIdx = contractIdx;
		this.moduleNamePrefix = moduleNamePrefix;
		this.dataSource = ic.dataSource();
		this.config = config;
	}

	/**
	 * 采样一局或多局（训练时段内随机时间切片），transition 汇入任务级 ExperienceStore。
	 *
	 * @param rng		随机源（seed 显式传入，同一 seed 可完全复现采样序列）
	 * @param episodes	本环境的局数配额
	 * @param epsilon	探索率（ε-greedy 在动作枚举层面由环境拦截，策略零 RL 簿记负担）
	 * @param round		当前轮次（分片命名用）
	 */
	public SampleStats sample(Random rng, int episodes, double epsilon, int round, ExperienceStore store) {
		double cumReward = 0;
		int transitions = 0;
		long totalDt = 0;
		for (int ep = 0; ep < episodes; ep++) {
			int startIdx = trainStartIdx + rng.nextInt(Math.max(1, validationStartIdx - trainStartIdx - 1));
			int length = Math.min(config.maxEpisodeBars(), validationStartIdx - startIdx);
			SampleStats stats = runEpisode(startIdx, length, epsilon, round, store, rng, false);
			cumReward += stats.cumulativeReward();
			transitions += stats.transitions();
			totalDt += stats.totalDt();
		}
		return new SampleStats(cumReward, transitions, totalDt, episodes);
	}

	/**
	 * 验证评估：在同一验证时段跑贪心策略（训练随机、评估固定，轮间指标才严格可比）。
	 * 模型未就绪（冷启动首轮）时返回 null。
	 */
	public EvalOutcome evaluate() {
		if (!agent.isReady()) {
			return null;
		}
		String moduleName = moduleNamePrefix + "-eval";
		RLModuleSandbox sandbox = RLModuleSandbox.create(moduleName, strategyTemplate, contract,
				config.initBalance(), 1, dataSource);
		replay.locate(validationStartIdx, replay.size() - validationStartIdx, config.warmupBars());
		runLoop(sandbox, 0.0, 0, null, new Random(0), true);
		return new EvalOutcome(sandbox.accountRepo().findDealRecords(moduleName),
				config.initBalance(), sandbox.equity());
	}

	private SampleStats runEpisode(int startIdx, int length, double epsilon, int round,
			ExperienceStore store, Random rng, boolean greedy) {
		String moduleName = "%s-r%d-c%d".formatted(moduleNamePrefix, round, contractIdx);
		RLModuleSandbox sandbox = RLModuleSandbox.create(moduleName, strategyTemplate, contract,
				config.initBalance(), 1, dataSource);
		replay.locate(startIdx, length, config.warmupBars());
		return runLoop(sandbox, epsilon, round, store, rng, greedy);
	}

	/**
	 * 环境主循环（step 语义 = "执行动作，推进到下一个决策点"）：
	 * <pre>
	 * s = observe(); a = agent(s)（训练时 ε-greedy）; onAction(a) 下单
	 * loop: 推进一根 bar（指标照常更新）→ 累积浮盈 → if shouldObserve() 或 强制触发点: break
	 * s' = observe(); R = 区间累计收益 - 交易成本; 记录 transition (s, a, R, s', dt, terminated)
	 * </pre>
	 */
	private SampleStats runLoop(RLModuleSandbox sandbox, double epsilon, int round,
			ExperienceStore store, Random rng, boolean greedy) {
		RLStrategy strategy = sandbox.strategy();
		IModuleStrategyContext ctx = sandbox.context();
		sandbox.setEnabled(false);

		float[] s = null;
		TradeAction a = null;
		double prevEquity = 0;
		int decisionCursor = -1;
		double cumReward = 0;
		int transitions = 0;
		long totalDt = 0;

		AdvanceResult r;
		while ((r = replay.advanceBar()) != AdvanceResult.EXHAUSTED) {
			sandbox.feed(replay.currentBar());
			if (r == AdvanceResult.WARMUP) {
				continue;	// 预热段：只喂数据不采样、不计 dt、不计奖励
			}
			if (!sandbox.context().isEnabled()) {
				sandbox.setEnabled(true);
			}
			if (s == null) {	// 首个正式 bar 起等待初始决策点
				// 预热第四层收口：初始观测推迟到指标稳定（复用策略 shouldObserve 的就绪守卫），
				// maxHoldingBars 强触发兜底防死行情饿死采样
				if (decisionCursor < 0) {
					decisionCursor = replay.cursor();	// 首个正式 bar：等待起点
				}
				if (replay.cursor() - decisionCursor < config.maxHoldingBars()
						&& !strategy.shouldObserve(ctx, contract)) {
					continue;
				}
				s = strategy.observe(ctx, contract);
				a = chooseAction(s, epsilon, rng, greedy);
				strategy.onAction(a, ctx, contract);
				prevEquity = sandbox.equity();
				decisionCursor = replay.cursor();
				continue;
			}
			int dt = replay.cursor() - decisionCursor;
			boolean ruin = sandbox.equity() < config.initBalance() * (1 - config.ruinThreshold());
			if (ruin || strategy.shouldObserve(ctx, contract) || dt >= config.maxHoldingBars()) {
				float[] ns = strategy.observe(ctx, contract);
				double currEquity = sandbox.equity();
				RLRewardContext rewardCtx = new RLRewardContext(prevEquity, currEquity,
						sandbox.drainIntervalTrades(), dt, ruin ? EpisodeEnd.TERMINATED : EpisodeEnd.NONE);
				double reward = strategy.reward(rewardCtx);
				if (store != null) {
					store.append(round, contractIdx, new Transition(
							toNetworkSpace(s), a.ordinal(), (float) reward, toNetworkSpace(ns), dt, ruin));
				}
				cumReward += reward;
				transitions++;
				totalDt += dt;
				if (ruin) {
					sandbox.forceClose();	// 风控触发：权益击穿为 terminated，回合提前结束
					return new SampleStats(cumReward, transitions, totalDt, 1);
				}
				s = ns;
				a = chooseAction(s, epsilon, rng, greedy);
				strategy.onAction(a, ctx, contract);
				prevEquity = currEquity;
				decisionCursor = replay.cursor();
			}
		}
		// 回合边界：数据耗尽强制平仓并记录最后一条 transition（truncated 语义）
		if (s != null) {
			sandbox.forceClose();
			int dt = Math.max(1, replay.cursor() - decisionCursor);
			float[] ns = strategy.observe(ctx, contract);
			double currEquity = sandbox.equity();
			double reward = strategy.reward(new RLRewardContext(prevEquity, currEquity,
					sandbox.drainIntervalTrades(), dt, EpisodeEnd.TRUNCATED));
			if (store != null) {
				store.append(round, contractIdx, new Transition(
						toNetworkSpace(s), a.ordinal(), (float) reward, toNetworkSpace(ns), dt, false));
			}
			cumReward += reward;
			transitions++;
			totalDt += dt;
		}
		return new SampleStats(cumReward, transitions, totalDt, 1);
	}

	/** 模型状态维度（用于经验数据契约校验） */
	public int stateDim() {
		return strategyTemplate.rlSpec().stateDim();
	}

	/** 训练前关闭 Agent 句柄（规避 Windows 文件锁：训练产物先写临时文件再原子切换） */
	public void markAgentStale() {
		agent.markStale();
	}

	/** 每轮训练完成后同步刷新权重（各 Agent 实例是同一份 model.pt 的只读副本） */
	public void reloadAgent() {
		agent.reload();
	}

	/** 关闭 Agent（任务结束时调用） */
	public void closeQuietly() {
		try {
			agent.close();
		} catch (Exception e) {
			log.warn("关闭 RLAgent 失败: {}", e.getMessage());
		}
	}

	/**
	 * 落盘前的状态空间转换：CUSTOM 时由策略预处理器变换到网络输入空间（Python 对 custom 直通）；
	 * identity/standardize 原样落盘（分别由 Python 冻结统计量 / 推理侧 Translator 处理）。
	 * 原始观测本身保持不变，继续作为主循环与推理侧的输入。
	 */
	private float[] toNetworkSpace(float[] raw) {
		return preprocessor != null ? preprocessor.apply(raw) : raw;
	}

	private TradeAction chooseAction(float[] state, double epsilon, Random rng, boolean greedy) {		TradeAction[] actions = TradeAction.values();
		if (greedy && agent.isReady()) {
			return agent.act(state);
		}
		if (!agent.isReady() || rng.nextDouble() < epsilon) {
			return actions[rng.nextInt(actions.length)];	// 探索（含冷启动纯随机）
		}
		return agent.act(state);	// 利用
	}
}
