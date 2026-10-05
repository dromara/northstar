package org.dromara.northstar.app.rl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.rl.RLRoundMetrics;
import org.dromara.northstar.common.model.rl.RLTrainingStatus;
import org.dromara.northstar.rl.experience.ExperienceStore;
import org.dromara.northstar.rl.model.RLModelStore;
import org.dromara.northstar.rl.model.RLVersionMetadata;
import org.dromara.northstar.rl.training.DqnHyperParams;
import org.dromara.northstar.rl.training.RLTrainExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * RL 训练 Runner：一个训练任务对应一个 Runner，按轮驱动「采样 -> Python 训练 -> 评估」循环。
 *
 * 设计要点：
 * <ul>
 *   <li>轮边界控制：暂停/继续/停止与超参数调整都在轮边界生效（避免一轮训练中途状态撕裂）</li>
 *   <li>串行队列：多任务通过全局信号量串行执行训练轮（v1 不做并发资源仲裁），暂停时释放信号量</li>
 *   <li>评估固定：每 evalInterval 轮在相同验证时段跑贪心策略，轮间指标严格可比</li>
 * </ul>
 */
@Slf4j
public class RLTrainingRunner {

	/** 验证评估间隔（每 N 轮一次 + 最后一轮） */
	public static final int EVAL_INTERVAL = 5;

	/** 多任务串行队列：全局训练信号量（按轮获取/释放，暂停任务不占用训练资源） */
	private final Semaphore trainLock;

	private final RLTrainingTaskRecord record;
	private final List<TradingEnvironment> envs;
	private final RLModelStore modelStore;
	private final RLTrainExecutor trainExecutor;
	private final String modelId;
	/** 重读任务最新状态（轮边界调参：UI 修改参数下一轮生效） */
	private final Supplier<RLTrainingTaskRecord> stateRefresher;
	/** 持久化任务状态 */
	private final Consumer<RLTrainingTaskRecord> statePersister;
	/** 轮指标/状态变更通知（SSE 推送） */
	private final Consumer<RLTrainingTaskRecord> stateNotifier;

	private volatile boolean pauseRequested;
	private volatile boolean stopRequested;
	/** 训练循环的终止句柄（删除任务前据此等待循环退出，避免滞后落库复活已删记录） */
	private volatile CompletableFuture<Void> termination;

	public RLTrainingRunner(RLTrainingTaskRecord record, List<TradingEnvironment> envs,
			RLModelStore modelStore, RLTrainExecutor trainExecutor, Semaphore trainLock,
			Supplier<RLTrainingTaskRecord> stateRefresher,
			Consumer<RLTrainingTaskRecord> statePersister,
			Consumer<RLTrainingTaskRecord> stateNotifier) {
		this.record = record;
		this.envs = envs;
		this.modelStore = modelStore;
		this.trainExecutor = trainExecutor;
		this.trainLock = trainLock;
		this.modelId = record.getModelId();
		this.stateRefresher = stateRefresher;
		this.statePersister = statePersister;
		this.stateNotifier = stateNotifier;
	}

	public String taskId() {
		return record.getTaskId();
	}

	/** 日志展示名（模型名+基线→目标版本），供服务层日志使用 */
	public String displayName() {
		return record.displayName();
	}

	public void startAsync() {
		termination = CompletableFuture.runAsync(this::runLoop).exceptionally(e -> {
			if (isTaskDeleted()) {
				log.info("RL 训练任务 {} 已删除，训练循环退出", record.displayName());
				return null;
			}
			log.error("RL 训练任务 {} 异常终止", record.displayName(), e);
			record.setStatus(RLTrainingStatus.FAILED);
			record.setFailReason(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
			persistAndNotify();
			return null;
		});
	}

	/**
	 * 等待训练循环退出（删除任务前调用）。
	 *
	 * @return true = 循环已退出；false = 超时未退出（调用方继续删除，后续滞后落库由持久化层抑制）
	 */
	public boolean awaitTermination(Duration timeout) {
		CompletableFuture<Void> f = termination;
		if (f == null) {
			return true;
		}
		try {
			f.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
			return true;
		} catch (TimeoutException e) {
			return false;
		} catch (Exception e) {
			return true;	// 循环已异常终止
		}
	}

	public void pause() {
		pauseRequested = true;
	}

	public void resume() {
		pauseRequested = false;
		record.setStatus(RLTrainingStatus.RUNNING);
	}

	public void stop() {
		stopRequested = true;
		pauseRequested = false;
	}

	private void runLoop() {
		int targetVersion = record.getTargetVersion();
		try (ExperienceStore store = new ExperienceStore(
				modelStore.roundsDir(modelId, targetVersion), envs.get(0).stateDim())) {
			while (record.getCurrentRound() < record.getTotalRounds()) {
				if (stopRequested) {
					break;
				}
				if (awaitIfPaused()) {
					break;	// 暂停期间被停止
				}
				trainLock.acquire();
				try {
					runRound(store);
				} finally {
					trainLock.release();
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			if (isTaskDeleted()) {
				log.info("RL 训练任务 {} 已删除，训练循环退出", record.displayName());
			} else {
				log.error("RL 训练任务 {} 失败", record.displayName(), e);
				record.setStatus(RLTrainingStatus.FAILED);
				record.setFailReason(e.getMessage());
			}
		} finally {
			envs.forEach(TradingEnvironment::closeQuietly);
		}

		if (record.getStatus() != RLTrainingStatus.FAILED) {
			// 正常跑完或被人工停止都视为完结（提前停止 = 以较少轮数完成训练）
			record.setStatus(RLTrainingStatus.FINISHED);
			writeAcceptedBy();
		}
		persistAndNotify();
	}

	/** 任务记录是否已被删除（删除后训练循环应尽快安静退出，不再落库/推送） */
	private boolean isTaskDeleted() {
		try {
			stateRefresher.get();
			return false;
		} catch (IllegalArgumentException e) {
			return true;
		}
	}

	/** 等待暂停解除；返回 true 表示等待期间收到停止指令 */
	private boolean awaitIfPaused() throws InterruptedException {
		if (!pauseRequested || stopRequested) {
			return stopRequested;
		}
		record.setStatus(RLTrainingStatus.PAUSED);
		persistAndNotify();
		while (pauseRequested && !stopRequested) {
			Thread.sleep(200);
		}
		if (!stopRequested) {
			record.setStatus(RLTrainingStatus.RUNNING);
			persistAndNotify();
		}
		return stopRequested;
	}

	private void runRound(ExperienceStore store) throws Exception {
		// 轮边界重读最新超参数（UI 修改下一轮生效）
		RLTrainingTaskRecord latest = stateRefresher.get();
		Map<String, Double> hyperParams = latest.getHyperParams();
		record.setHyperParams(hyperParams);
		DqnHyperParams hp = resolveHyperParams(hyperParams);

		int round = record.getCurrentRound();
		double epsilon = hp.epsilonAt(round);
		double lr = hp.getLr();
		long roundSeed = (record.getSeed() + round) & 0xFFFFFFFFL; // 限定 [0, 2^32-1]，与 Python 端播种范围一致
		Random rng = new Random(roundSeed);

		// 1) 串行遍历环境采样（内存恒定）；episode 配额按余数分配：base 均分 + 前 remainder 个环境各 +1，
		//    环境数多于局数时前 episodesPerRound 个环境各 1 局、其余配额为 0 直接跳过采样
		double cumReward = 0;
		int transitions = 0;
		long totalDt = 0;
		int envIdx = 0;
		for (TradingEnvironment env : envs) {
			int quota = episodeQuota(record.getEpisodesPerRound(), envs.size(), envIdx++);
			if (quota == 0) {
				continue;
			}
			TradingEnvironment.SampleStats stats = env.sample(rng, quota, epsilon, round, store);
			cumReward += stats.cumulativeReward();
			transitions += stats.transitions();
			totalDt += stats.totalDt();
		}
		store.flush();

		// 2) Python 单轮训练：先关闭全部 Agent 句柄（规避 Windows 文件锁），训练完成后同步刷新
		envs.forEach(TradingEnvironment::markAgentStale);
		RLTrainExecutor.RLTrainResult result = trainExecutor.trainRound(
				modelStore.modelDir(modelId), targetVersion(), hp.gradSteps(transitions), lr, roundSeed);
		envs.forEach(TradingEnvironment::reloadAgent);

		// 3) 验证时段评估（训练随机、评估固定）
		Double evalWinRate = null, evalProfitFactor = null, evalReturn = null;
		Integer evalTrades = null;
		if ((round + 1) % EVAL_INTERVAL == 0 || round + 1 == record.getTotalRounds()) {
			List<DealRecord> pooledDeals = new ArrayList<>();
			double totalInit = 0;
			double totalEquity = 0;
			for (TradingEnvironment env : envs) {
				TradingEnvironment.EvalOutcome outcome = env.evaluate();
				if (outcome != null) {
					pooledDeals.addAll(outcome.deals());
					totalInit += outcome.initBalance();
					totalEquity += outcome.finalEquity();
				}
			}
			if (!pooledDeals.isEmpty() || totalInit > 0) {
				evalWinRate = winRate(pooledDeals);
				evalProfitFactor = profitFactor(pooledDeals);
				evalTrades = pooledDeals.size();
				evalReturn = totalInit > 0 ? (totalEquity / totalInit - 1) * 100 : null;
			}
		}

		RLRoundMetrics metrics = RLRoundMetrics.builder()
				.round(round + 1)
				.avgReward(transitions > 0 ? cumReward / transitions : 0)
				.cumulativeReward(cumReward)
				.loss(result.loss())
				.epsilon(epsilon)
				.lr(lr)
				.avgDecisionInterval(transitions > 0 ? (double) totalDt / transitions : 0)
				.evalWinRate(evalWinRate)
				.evalProfitFactor(evalProfitFactor)
				.evalTrades(evalTrades)
				.evalReturn(evalReturn)
				.build();
		record.getMetrics().add(metrics);
		record.setCurrentRound(round + 1);
		log.info("RL 任务 {} 第 {} 轮完成：avgReward={} loss={} 评估={}",
				record.displayName(), round + 1, metrics.getAvgReward(), metrics.getLoss(),
				evalWinRate == null ? "跳过" : "胜率%.2f%%".formatted(evalWinRate * 100));
		// 轮末持久化前对齐库中最新状态：吸收轮中调参（避免整记录覆写造成丢失更新），
		// 人工停止（FINISHED 已落库）后不得把状态回写为 RUNNING；任务已被删除则本轮结算后安静退出
		try {
			RLTrainingTaskRecord latestState = stateRefresher.get();
			record.setHyperParams(latestState.getHyperParams());
			if (latestState.getStatus() == RLTrainingStatus.FINISHED) {
				record.setStatus(RLTrainingStatus.FINISHED);
			}
			persistAndNotify();
		} catch (IllegalArgumentException e) {
			stopRequested = true;
			log.info("RL 任务 {} 已被删除，第 {} 轮结算后退出", record.displayName(), round + 1);
		}
	}

	/**
	 * episode 配额余数分配：base = episodesPerRound / envCount 均分，前 episodesPerRound % envCount 个环境各 +1。
	 * 环境数多于局数时靠后的环境配额为 0（调用方跳过采样）。
	 */
	static int episodeQuota(int episodesPerRound, int envCount, int envIndex) {
		return episodesPerRound / envCount + (envIndex < episodesPerRound % envCount ? 1 : 0);
	}

	/**
	 * 训练达标判定写入版本 metadata（§5.6 达标标准：胜率>50% 且 盈亏比>2 且 交易笔数>=100）。
	 * 仅在最后一次评估三项同时达标时写入 acceptedBy；不达标或无评估指标则不写，版本保持"未达标"状态。
	 * 盈亏比为 null 表示"无亏损"（见 profitFactor 语义注释）：分母为 0 数学上即 +∞，视为盈亏比达标。
	 */
	void writeAcceptedBy() {
		record.getMetrics().stream()
				.filter(m -> m.getEvalWinRate() != null)
				.reduce((a, b) -> b)
				.ifPresentOrElse(m -> {
					if (!meetsAcceptanceCriteria(m)) {
						log.info("RL 任务 {} 未达标（胜率={} 盈亏比={} 交易笔数={}），版本 v{} 不写入达标标记",
								record.displayName(), m.getEvalWinRate(), m.getEvalProfitFactor(),
								m.getEvalTrades(), record.getTargetVersion());
						return;
					}
					try {
						RLVersionMetadata verMeta = modelStore.findVersion(modelId, record.getTargetVersion()).orElseThrow();
						verMeta.setAcceptedBy(new RLVersionMetadata.AcceptedBy(
								m.getEvalWinRate(), acceptedByProfitFactor(m), m.getEvalTrades()));
						verMeta.save(modelStore.versionDir(modelId, record.getTargetVersion()));
					} catch (Exception e) {
						log.warn("写入版本评估指标失败: {}", e.getMessage());
					}
				}, () -> log.info("RL 任务 {} 无评估指标，版本 v{} 不写入达标标记", record.displayName(), record.getTargetVersion()));
	}

	/**
	 * 达标三阈值判定（§5.6）：胜率>50% 且 交易笔数>=100 且（盈亏比>2 或 无亏损=null 视为达标）。
	 */
	static boolean meetsAcceptanceCriteria(RLRoundMetrics m) {
		return m.getEvalWinRate() > 0.5
				&& m.getEvalTrades() != null && m.getEvalTrades() >= 100
				&& (m.getEvalProfitFactor() == null || m.getEvalProfitFactor() > 2);
	}

	/**
	 * acceptedBy 落盘的盈亏比取值：AcceptedBy.profitFactor 为基本类型且 Gson 不支持非有限值，
	 * 无亏损（数学上为 +∞）时封顶写入一个远大于达标线的有限值，复盘时与胜率=100% 互证。
	 */
	private static double acceptedByProfitFactor(RLRoundMetrics m) {
		return m.getEvalProfitFactor() == null ? 1_000_000d : m.getEvalProfitFactor();
	}

	static double winRate(List<DealRecord> deals) {
		if (deals.isEmpty()) {
			return 0;
		}
		return (double) deals.stream().filter(d -> d.getDealProfit() > 0).count() / deals.size();
	}

	/**
	 * 平均盈亏比 = 平均盈利 / |平均亏损|。
	 * 无亏损（lossAvg == 0，含全胜与零交易）时返回 null 表示"不可比"：分母为 0 数学上是 +∞，
	 * 但 Infinity 经 SSE/REST（Jackson 3）会序列化成字符串 "Infinity"，前端按数值消费直接报错；
	 * null 与既有"未评估"空值口径一致，前端按占位符展示。与 BacktestRunner 的 profitLossRatio 口径保持一致。
	 */
	static Double profitFactor(List<DealRecord> deals) {
		double profitAvg = deals.stream().filter(d -> d.getDealProfit() > 0)
				.mapToDouble(DealRecord::getDealProfit).average().orElse(0);
		double lossAvg = deals.stream().filter(d -> d.getDealProfit() <= 0)
				.mapToDouble(DealRecord::getDealProfit).average().orElse(0);
		return lossAvg == 0 ? null : profitAvg / Math.abs(lossAvg);
	}

	private static DqnHyperParams resolveHyperParams(Map<String, Double> hyperParams) {
		try {
			DqnHyperParams hp = new DqnHyperParams();
			Collection<ComponentField> fields = hp.getMetaInfo();
			fields.forEach(f -> {
				Double v = hyperParams.get(f.getName());
				if (v != null) {
					f.setValue(v);
				}
			});
			hp.resolveFromSource(fields);
			return hp;
		} catch (Exception e) {
			throw new IllegalStateException("超参数解析失败: " + e.getMessage(), e);
		}
	}

	private int targetVersion() {
		return record.getTargetVersion();
	}

	private void persistAndNotify() {
		statePersister.accept(record);
		stateNotifier.accept(record);
	}
}
