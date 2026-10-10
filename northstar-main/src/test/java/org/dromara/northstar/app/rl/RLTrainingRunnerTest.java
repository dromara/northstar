package org.dromara.northstar.app.rl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.rl.RLRoundMetrics;
import org.dromara.northstar.common.model.rl.RLTrainingStatus;
import org.dromara.northstar.rl.model.RLModelStore;
import org.dromara.northstar.rl.model.RLVersionMetadata;
import org.dromara.northstar.rl.training.RLAlgorithms;
import org.dromara.northstar.rl.training.RLTrainExecutor;
import org.dromara.northstar.strategy.rl.RLAlgo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * RLTrainingRunner 单测：episode 配额余数分配、串行信号量、轮边界调参、
 * 评估节奏、winRate/profitFactor 纯函数与 writeAcceptedBy 三阈值判定。
 * 环境/模型仓库/训练执行器全部以 mock 替代，不触碰真实行情与 Python 进程。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RLTrainingRunnerTest {

	@TempDir
	Path tmp;

	@Mock
	RLModelStore modelStore;
	@Mock
	RLTrainExecutor trainExecutor;
	@Mock
	TradingEnvironment env0;
	@Mock
	TradingEnvironment env1;

	private RLTrainingTaskRecord record;

	@BeforeEach
	void setUp() throws Exception {
		record = RLTrainingTaskRecord.builder()
				.taskId("t1")
				.modelId("m")
				.modelName("dqn-demo")
				.targetVersion(1)
				.totalRounds(2)
				.episodesPerRound(2)
				.hyperParams(RLAlgorithms.hyperParamDefaults(RLAlgo.DQN))
				.status(RLTrainingStatus.RUNNING)
				.currentRound(0)
				.seed(1)
				.metrics(new ArrayList<>())
				.build();
		when(modelStore.roundsDir("m", 1)).thenReturn(tmp.resolve("rounds"));
		when(modelStore.modelDir("m")).thenReturn(tmp);
		for (TradingEnvironment env : List.of(env0, env1)) {
			when(env.stateDim()).thenReturn(6);
			when(env.sample(any(), anyInt(), anyDouble(), anyInt(), any()))
					.thenAnswer(inv -> new TradingEnvironment.SampleStats(10, 5, 25, inv.getArgument(1)));
			when(env.evaluate()).thenReturn(new TradingEnvironment.EvalOutcome(List.of(), 100_000, 100_000));
		}
		when(trainExecutor.trainRound(any(), anyInt(), anyInt(), any(), anyLong()))
				.thenReturn(new RLTrainExecutor.RLTrainResult(0.5, 300, 1000));
	}

	private RLTrainingRunner newRunner(List<TradingEnvironment> envs, Semaphore lock) {
		return new RLTrainingRunner(record, envs, modelStore, trainExecutor, lock,
				() -> record, _ -> { }, _ -> { });
	}

	private void runToEnd(RLTrainingRunner runner) {
		runner.startAsync();
		assertTrue(runner.awaitTermination(Duration.ofSeconds(30)), "训练循环应在超时前退出");
	}

	/* ---------- episode 配额余数分配 ---------- */

	@Test
	void episodeQuotaDistributesRemainderToLeadingEnvs() {
		// 7 局 2 环境 → 4 + 3
		assertEquals(4, RLTrainingRunner.episodeQuota(7, 2, 0));
		assertEquals(3, RLTrainingRunner.episodeQuota(7, 2, 1));
		// 1 局 2 环境 → 1 + 0（配额 0 的环境不采样）
		assertEquals(1, RLTrainingRunner.episodeQuota(1, 2, 0));
		assertEquals(0, RLTrainingRunner.episodeQuota(1, 2, 1));
		// 整除 → 均分
		assertEquals(2, RLTrainingRunner.episodeQuota(6, 3, 2));
		// 任意 n/m 组合下配额总和恒等于总局数
		for (int n = 1; n <= 20; n++) {
			for (int m = 1; m <= 6; m++) {
				int sum = 0;
				for (int i = 0; i < m; i++) {
					sum += RLTrainingRunner.episodeQuota(n, m, i);
				}
				assertEquals(n, sum, "n=%d m=%d 配额总和应等于总局数".formatted(n, m));
			}
		}
	}

	@Test
	void runRoundAppliesQuotaPerEnv() {
		record.setEpisodesPerRound(7);
		record.setTotalRounds(1);
		runToEnd(newRunner(List.of(env0, env1), new Semaphore(1)));

		verify(env0).sample(any(), eq(4), anyDouble(), eq(0), any());
		verify(env1).sample(any(), eq(3), anyDouble(), eq(0), any());
	}

	@Test
	void zeroQuotaEnvIsSkipped() {
		record.setEpisodesPerRound(1);
		record.setTotalRounds(1);
		runToEnd(newRunner(List.of(env0, env1), new Semaphore(1)));

		verify(env0).sample(any(), eq(1), anyDouble(), eq(0), any());
		verify(env1, never()).sample(any(), anyInt(), anyDouble(), anyInt(), any());
	}

	/* ---------- 串行信号量 ---------- */

	/** 记录 acquire/release 次数的信号量（验证按轮持有、轮边界释放） */
	static class TrackingSemaphore extends Semaphore {
		private static final long serialVersionUID = 1L;
		final AtomicInteger acquires = new AtomicInteger();
		final AtomicInteger releases = new AtomicInteger();

		TrackingSemaphore(int permits) {
			super(permits);
		}

		@Override
		public void acquire() throws InterruptedException {
			super.acquire();
			acquires.incrementAndGet();
		}

		@Override
		public void release() {
			releases.incrementAndGet();
			super.release();
		}
	}

	@Test
	void semaphoreHeldPerRoundAndReleasedAtRoundBoundary() {
		record.setTotalRounds(3);
		TrackingSemaphore lock = new TrackingSemaphore(1);
		runToEnd(newRunner(List.of(env0), lock));

		assertEquals(3, lock.acquires.get(), "每轮获取一次许可");
		assertEquals(3, lock.releases.get(), "每轮结束释放许可");
		assertEquals(1, lock.availablePermits(), "循环退出后不持有许可");
	}

	@Test
	void pausedTaskDoesNotHoldSemaphore() throws Exception {
		record.setTotalRounds(3);
		TrackingSemaphore lock = new TrackingSemaphore(1);
		RLTrainingRunner runner = newRunner(List.of(env0), lock);
		runner.pause();
		runner.startAsync();

		// 暂停期间训练循环在轮边界等待，不应占用训练信号量
		Thread.sleep(500);
		assertEquals(0, lock.acquires.get(), "暂停时不获取许可");
		assertEquals(1, lock.availablePermits(), "暂停时不持有许可");

		runner.stop();
		assertTrue(runner.awaitTermination(Duration.ofSeconds(30)));
		assertEquals(0, lock.acquires.get(), "暂停中被停止，全程未获取许可");
	}

	/* ---------- 轮边界调参 ---------- */

	@Test
	void hyperParamChangeAtRoundBoundaryTakesEffectNextRound() throws Exception {
		record.setTotalRounds(2);
		List<Double> lrUsed = new ArrayList<>();
		when(trainExecutor.trainRound(any(), anyInt(), anyInt(), any(), anyLong())).thenAnswer(inv -> {
			lrUsed.add(inv.getArgument(3));
			// 第一轮训练期间修改超参数（模拟 UI 轮边界调参），第二轮应生效
			record.getHyperParams().put("epsilonStart", 0.0);
			record.getHyperParams().put("lr", 0.01);
			return new RLTrainExecutor.RLTrainResult(0.5, 300, 1000);
		});

		runToEnd(newRunner(List.of(env0), new Semaphore(1)));

		List<RLRoundMetrics> metrics = record.getMetrics();
		assertEquals(2, metrics.size());
		assertEquals(1.0, metrics.get(0).getEpsilon(), "第一轮用旧探索率");
		assertEquals(0.05, metrics.get(1).getEpsilon(), "第二轮探索率随调参变化（epsilonStart=0 退化为下限）");
		assertEquals(List.of(5e-4, 0.01), lrUsed, "第二轮学习率随调参变化");
	}

	/* ---------- 评估节奏 ---------- */

	@Test
	void evaluationRunsAtIntervalAndFinalRound() {
		record.setTotalRounds(6);
		when(env0.evaluate()).thenReturn(new TradingEnvironment.EvalOutcome(List.of(), 100_000, 101_000));

		runToEnd(newRunner(List.of(env0), new Semaphore(1)));

		List<RLRoundMetrics> metrics = record.getMetrics();
		assertEquals(6, metrics.size());
		for (int i = 0; i < 4; i++) {
			assertNull(metrics.get(i).getEvalWinRate(), "第 %d 轮不评估".formatted(i + 1));
		}
		for (int i : new int[] {4, 5}) {	// 第 5 轮（EVAL_INTERVAL 整数倍）与末轮评估
			RLRoundMetrics m = metrics.get(i);
			assertNotNull(m.getEvalWinRate(), "第 %d 轮应产出评估指标".formatted(i + 1));
			assertEquals(0, m.getEvalTrades(), "零交易评估");
			assertNull(m.getEvalProfitFactor(), "零交易无亏损，盈亏比为 null");
			assertEquals(1.0, m.getEvalReturn(), 1e-9, "验证时段收益率 1%");
		}
		verify(env0, times(2)).evaluate();
	}

	/* ---------- winRate / profitFactor 纯函数 ---------- */

	private static DealRecord deal(double profit) {
		return DealRecord.builder().dealProfit(profit).build();
	}

	@Test
	void winRateAndProfitFactorBoundaries() {
		assertEquals(0, RLTrainingRunner.winRate(List.of()), "零交易胜率为 0");
		assertEquals(2.0 / 3, RLTrainingRunner.winRate(List.of(deal(100), deal(-50), deal(200))), 1e-9);

		assertNull(RLTrainingRunner.profitFactor(List.of()), "零交易无亏损，盈亏比为 null");
		assertNull(RLTrainingRunner.profitFactor(List.of(deal(100), deal(200))), "全胜无亏损，盈亏比为 null");
		assertEquals(0.0, RLTrainingRunner.profitFactor(List.of(deal(-100), deal(-50))), "全亏时盈亏比为 0");
		// 平均盈利 150 / 平均亏损 100 = 1.5
		assertEquals(1.5, RLTrainingRunner.profitFactor(List.of(deal(200), deal(100), deal(-100))), 1e-9);
	}

	/* ---------- writeAcceptedBy 三阈值判定 ---------- */

	private static RLRoundMetrics evalMetrics(double winRate, Double profitFactor, Integer trades) {
		return RLRoundMetrics.builder()
				.evalWinRate(winRate)
				.evalProfitFactor(profitFactor)
				.evalTrades(trades)
				.build();
	}

	@Test
	void meetsAcceptanceCriteriaChecksThreeThresholds() {
		assertTrue(RLTrainingRunner.meetsAcceptanceCriteria(evalMetrics(0.6, 3.0, 150)));
		assertTrue(RLTrainingRunner.meetsAcceptanceCriteria(evalMetrics(1.0, null, 100)), "无亏损视为盈亏比达标");
		assertFalse(RLTrainingRunner.meetsAcceptanceCriteria(evalMetrics(0.5, 3.0, 150)), "胜率须严格大于 50%");
		assertFalse(RLTrainingRunner.meetsAcceptanceCriteria(evalMetrics(0.6, 2.0, 150)), "盈亏比须严格大于 2");
		assertFalse(RLTrainingRunner.meetsAcceptanceCriteria(evalMetrics(0.6, 3.0, 99)), "交易笔数须 >= 100");
		assertFalse(RLTrainingRunner.meetsAcceptanceCriteria(evalMetrics(0.6, 3.0, null)), "无交易笔数不达标");
	}

	@Test
	void writeAcceptedByPersistsWhenAccepted() throws Exception {
		record.getMetrics().add(evalMetrics(0.6, 3.0, 150));
		RLVersionMetadata verMeta = new RLVersionMetadata();
		verMeta.setVersion(1);
		when(modelStore.findVersion("m", 1)).thenReturn(Optional.of(verMeta));
		when(modelStore.versionDir("m", 1)).thenReturn(tmp.resolve("v1"));

		newRunner(List.of(env0), new Semaphore(1)).writeAcceptedBy();

		RLVersionMetadata saved = RLVersionMetadata.load(tmp.resolve("v1"));
		assertNotNull(saved.getAcceptedBy());
		assertEquals(0.6, saved.getAcceptedBy().getWinRate());
		assertEquals(3.0, saved.getAcceptedBy().getProfitFactor());
		assertEquals(150, saved.getAcceptedBy().getTrades());
	}

	@Test
	void writeAcceptedByCapsNullProfitFactor() throws Exception {
		// 全胜无亏损：profitFactor 为 null（数学上 +∞），落盘封顶为有限值
		record.getMetrics().add(evalMetrics(1.0, null, 200));
		RLVersionMetadata verMeta = new RLVersionMetadata();
		verMeta.setVersion(1);
		when(modelStore.findVersion("m", 1)).thenReturn(Optional.of(verMeta));
		when(modelStore.versionDir("m", 1)).thenReturn(tmp.resolve("v1"));

		newRunner(List.of(env0), new Semaphore(1)).writeAcceptedBy();

		RLVersionMetadata saved = RLVersionMetadata.load(tmp.resolve("v1"));
		assertNotNull(saved.getAcceptedBy());
		assertEquals(1_000_000d, saved.getAcceptedBy().getProfitFactor());
	}

	@Test
	void writeAcceptedBySkipsWhenNotAccepted() {
		record.getMetrics().add(evalMetrics(0.4, 3.0, 150));

		newRunner(List.of(env0), new Semaphore(1)).writeAcceptedBy();

		verify(modelStore, never()).findVersion(anyString(), anyInt());
	}

	@Test
	void writeAcceptedBySkipsWhenNoEvaluation() {
		record.getMetrics().add(RLRoundMetrics.builder().round(1).build());

		newRunner(List.of(env0), new Semaphore(1)).writeAcceptedBy();

		verify(modelStore, never()).findVersion(anyString(), anyInt());
	}

	@Test
	void writeAcceptedByUsesLastEvaluation() throws Exception {
		// 中间轮评估达标但最后一次不达标：以最后一次评估为准，不写入
		record.getMetrics().add(evalMetrics(0.6, 3.0, 150));
		record.getMetrics().add(RLRoundMetrics.builder().round(2).build());	// 非评估轮
		record.getMetrics().add(evalMetrics(0.6, 1.5, 150));				// 末次评估盈亏比不达标

		newRunner(List.of(env0), new Semaphore(1)).writeAcceptedBy();

		verify(modelStore, never()).findVersion(anyString(), anyInt());
	}

	@Test
	void fullLoopEndsFinishedAndPersistsMetrics() {
		record.setTotalRounds(2);
		runToEnd(newRunner(List.of(env0, env1), new Semaphore(1)));

		assertEquals(RLTrainingStatus.FINISHED, record.getStatus());
		assertEquals(2, record.getCurrentRound());
		assertEquals(2, record.getMetrics().size());
		assertEquals(0.5, record.getMetrics().get(0).getLoss());
	}
}
