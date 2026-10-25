package org.dromara.northstar.app.rl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.rl.RLTrainingStatus;
import org.dromara.northstar.common.model.rl.RLTrainingTaskDTO;
import org.dromara.northstar.common.model.rl.RLTrainingTaskVO;
import org.dromara.northstar.data.jdbc.RLTrainingTaskRepository;
import org.dromara.northstar.data.jdbc.entity.RLTrainingTaskDO;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.rl.model.RLModelMetadata;
import org.dromara.northstar.rl.model.RLModelStore;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.example.RlTestStrategy;
import org.dromara.northstar.strategy.rl.RLAlgo;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RLTrainingServiceTest {

	@TempDir
	Path modelsRoot;

	@Mock
	RLTrainingTaskRepository repo;
	@Mock
	ApplicationContext appCtx;
	@Mock
	EventEmitterHolder emitterHolder;
	@Mock
	MarketCenter mktCenter;
	@Mock
	RLTrainingDataManager dataManager;

	RLModelStore modelStore;
	TestableService service;
	RlTestStrategy strategy = new RlTestStrategy();

	/** 截获 runner 构建（同步），避免真实数据加载与异步训练 */
	static class TestableService extends RLTrainingService {
		Function<RLTrainingTaskRecord, RLTrainingRunner> runnerBuilder = _ -> mock(RLTrainingRunner.class);
		List<RLTrainingTaskRecord> built = new ArrayList<>();

		@Override
		RLTrainingRunner buildRunner(RLTrainingTaskRecord record) {
			built.add(record);
			return runnerBuilder.apply(record);
		}
	}

	@BeforeEach
	void setUp() {
		modelStore = new RLModelStore(modelsRoot);
		modelStore.syncModel(strategy);

		service = new TestableService();
		service.repo = repo;
		service.appCtx = appCtx;
		service.emitterHolder = emitterHolder;
		service.modelStore = modelStore;
		service.trainExecutor = new StubTrainExecutor();
		service.mktCenter = mktCenter;
		service.dataManager = dataManager;

		when(appCtx.getBeansWithAnnotation(StrategicComponent.class))
				.thenAnswer(_ -> Map.of("RlTestStrategy", strategy));
	}

	private RLTrainingTaskDTO dto() {
		return RLTrainingTaskDTO.builder()
				.strategyClass(RlTestStrategy.class.getName())
				.modelId("dqn-demo")
				.baseVersion(0)
				.contracts(List.of("rb0000@SHFE@FUTURES"))
				.warmupStartDate("2024-01-01")
				.trainStartDate("2024-02-01")
				.validationStartDate("2024-03-01")
				.totalRounds(2)
				.episodesPerRound(2)
				.initBalance(100_000d)
				.maxHoldingBars(240)
				.ruinThreshold(0.5)
				.maxEpisodeBars(2000)
				.defaultVolume(1.0)
				.hyperParams(new HashMap<>(Map.of("lr", 0.001)))
				.build();
	}

	@Test
	void rlStrategiesOnlyListsRLStrategyImplementations() {
		var list = service.rlStrategies();
		assertEquals(1, list.size());
		assertEquals(RlTestStrategy.class.getName(), list.get(0).getClassName());
		assertEquals("RL测试策略", list.get(0).getName());
	}

	@Test
	void modelsIncludeHyperParamFieldsAndVersions() {
		var models = service.models();
		assertEquals(1, models.size());
		var model = models.get(0);
		assertEquals("dqn-demo", model.getModelId());
		assertEquals(RLAlgo.DQN, model.getAlgo());
		assertNull(model.getNetwork(), "无已训练版本时 Python 尚未回写 network，应为 null");
		assertEquals(10, model.getHyperParamFields().size(), "DQN 超参数字段清单");
		assertEquals(0, model.getLatestVersion());
		assertTrue(model.getVersionDetails().isEmpty());
	}

	@Test
	void modelsNetworkComesFromLatestVersionMetadata() throws Exception {
		service.createTask(dto());	// fork v1
		var verMeta = modelStore.findVersion("dqn-demo", 1).orElseThrow();
		verMeta.setNetwork(Map.of("type", "mlp", "hidden", List.of(128, 128), "activation", "relu"));
		verMeta.save(modelStore.versionDir("dqn-demo", 1));

		var model = service.models().get(0);
		assertEquals(List.of(128, 128), model.getNetwork().getHidden(), "network 透传自版本级 metadata");
		assertEquals("relu", model.getNetwork().getActivation());
	}

	@Test
	void createTaskValidatesDateOrder() {
		RLTrainingTaskDTO bad = dto();
		bad.setTrainStartDate("2024-01-01");
		bad.setWarmupStartDate("2024-02-01");
		assertThrows(IllegalArgumentException.class, () -> service.createTask(bad));
	}

	@Test
	void createTaskRejectsNonRLStrategy() {
		RLTrainingTaskDTO bad = dto();
		bad.setStrategyClass("org.example.NotExists");
		assertThrows(IllegalArgumentException.class, () -> service.createTask(bad));
	}

	@Test
	void createTaskRejectsUnknownHyperParam() {
		RLTrainingTaskDTO bad = dto();
		bad.getHyperParams().put("notAParam", 1.0);
		assertThrows(IllegalArgumentException.class, () -> service.createTask(bad));
	}

	@Test
	void createTaskValidatesSandboxParams() {
		RLTrainingTaskDTO noBalance = dto();
		noBalance.setInitBalance(0.0);
		assertThrows(IllegalArgumentException.class, () -> service.createTask(noBalance));

		RLTrainingTaskDTO noHoldingBars = dto();
		noHoldingBars.setMaxHoldingBars(0);
		assertThrows(IllegalArgumentException.class, () -> service.createTask(noHoldingBars));

		RLTrainingTaskDTO badRuin = dto();
		badRuin.setRuinThreshold(1.0);
		assertThrows(IllegalArgumentException.class, () -> service.createTask(badRuin));

		RLTrainingTaskDTO badEpisodeBars = dto();
		badEpisodeBars.setMaxEpisodeBars(0);
		assertThrows(IllegalArgumentException.class, () -> service.createTask(badEpisodeBars));

		RLTrainingTaskDTO badVolume = dto();
		badVolume.setDefaultVolume(0.0);
		assertThrows(IllegalArgumentException.class, () -> service.createTask(badVolume));
	}

	@Test
	void createTaskForksVersionWithoutLoadingOrTraining() {
		RLTrainingTaskVO vo = service.createTask(dto());

		assertEquals(RLTrainingStatus.CREATED, vo.getStatus(), "创建后处于待加载状态");
		assertEquals(1, vo.getTargetVersion(), "从零训练创建 v1");
		assertEquals(RLAlgo.DQN, vo.getAlgo());
		assertEquals(0.001, vo.getHyperParams().get("lr"));
		assertEquals(0.0, vo.getHyperParams().get("gamma"), "未覆盖项取默认值（0=自动，τ 对齐单局 bar 数）");
		assertTrue(modelStore.findVersion("dqn-demo", 1).isPresent());
		assertEquals(1, modelStore.findModel("dqn-demo").orElseThrow().getLatestVersion());
		long seed = modelStore.findVersion("dqn-demo", 1).orElseThrow().getTraining().getSeed();
		assertTrue(seed >= 0 && seed <= 0xFFFFFFFFL, "采样种子须在 [0, 2^32-1] 内（Python numpy 播种范围）");
		assertTrue(service.built.isEmpty(), "创建阶段不构建训练环境");
		verify(repo, atLeastOnce()).save(any(RLTrainingTaskDO.class));
		verifyNoInteractions(mktCenter, dataManager);
	}

	@Test
	void createTaskPersistenceFailureRollsBackFork() {
		doThrow(new IllegalStateException("数据库写入失败")).when(repo).save(any(RLTrainingTaskDO.class));

		assertThrows(IllegalStateException.class, () -> service.createTask(dto()));

		assertFalse(Files.exists(modelStore.versionDir("dqn-demo", 1)), "版本目录应被回收");
		assertEquals(0, modelStore.findModel("dqn-demo").orElseThrow().getLatestVersion());
	}

	@Test
	void createTaskBootstrapsWhenMetadataMissing() throws Exception {
		Files.delete(modelsRoot.resolve("dqn-demo").resolve(RLModelMetadata.FILE_NAME));

		RLTrainingTaskVO vo = service.createTask(dto());

		assertEquals(RLTrainingStatus.CREATED, vo.getStatus(), "metadata 缺失不得阻塞任务创建（bootstrap）");
		assertEquals(1, vo.getTargetVersion());
		RLModelMetadata rebuilt = modelStore.findModel("dqn-demo").orElseThrow();
		assertEquals(strategy.rlSpec().specHash(), rebuilt.getSpecHash(), "metadata 应由策略现场 spec 重建");
	}

	@Test
	void createTaskRewritesStaleMetadata() throws Exception {
		// 人为篡改模型 specHash 与特征清单，模拟陈旧 metadata：创建任务时应重写而非失败
		var model = modelStore.findModel("dqn-demo").orElseThrow();
		model.setSpecHash("deadbeefdeadbeef");
		var features = new ArrayList<>(model.getFeatures());
		features.set(0, new RLModelMetadata.FeatureInfo("ret1", "Z_SCORE"));
		model.setFeatures(features);
		try {
			model.save(modelsRoot.resolve("dqn-demo"));
		} catch (Exception e) {
			fail(e);
		}

		RLTrainingTaskVO vo = service.createTask(dto());

		assertEquals(RLTrainingStatus.CREATED, vo.getStatus());
		RLModelMetadata rewritten = modelStore.findModel("dqn-demo").orElseThrow();
		assertEquals(strategy.rlSpec().specHash(), rewritten.getSpecHash(), "陈旧 metadata 应重写为现场 spec");
		assertEquals("RATIO", rewritten.getFeatures().get(0).getNormalizer());
	}

	@Test
	void createTaskRejectsMismatchedModelId() {
		RLTrainingTaskDTO bad = dto();
		bad.setModelId("another-model");
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.createTask(bad));
		assertTrue(e.getMessage().contains("dqn-demo"), "报错应说明策略绑定的模型名: " + e.getMessage());
	}

	@Test
	void createTaskWritesTriggerSummaryToVersionMetadata() {
		RLTrainingTaskVO vo = service.createTask(dto());

		var verMeta = modelStore.findVersion("dqn-demo", vo.getTargetVersion()).orElseThrow();
		String triggerSummary = verMeta.getTraining().getTriggerSummary();
		assertNotNull(triggerSummary, "触发条件摘要应写入版本 metadata（供复盘）");
		assertTrue(triggerSummary.contains("决策点超时强触发=240bar"), triggerSummary);
		assertTrue(triggerSummary.contains(RlTestStrategy.class.getName()), triggerSummary);
	}

	/* ---------- 数据加载 ---------- */

	@Test
	void loadDataTransitionsToReady() throws Exception {
		RLTrainingTaskVO vo = service.createTask(dto());
		persistedAs(record(vo));
		when(dataManager.dataExists(eq(vo.getTaskId()), anyString())).thenReturn(false);
		when(dataManager.fetchAndSave(any(), any(), eq(vo.getTaskId()), anyString())).thenReturn(100);
		when(mktCenter.getContract(eq(ChannelType.PLAYBACK), anyString())).thenReturn(mock(IContract.class));

		RLTrainingTaskVO loading = service.loadData(vo.getTaskId());
		assertEquals(RLTrainingStatus.LOADING, loading.getStatus());

		verify(repo, timeout(3000).atLeastOnce()).save(argThat(doObj -> {
			RLTrainingTaskRecord r = doObj.convertTo();
			return r.getStatus() == RLTrainingStatus.READY
					&& r.getDataLoadedContracts() == 1
					&& r.getDataLoadedBars() == 100;
		}));
	}

	@Test
	void loadDataSkipsExistingCsv() {
		RLTrainingTaskVO vo = service.createTask(dto());
		persistedAs(record(vo));
		when(dataManager.dataExists(eq(vo.getTaskId()), anyString())).thenReturn(true);

		service.loadData(vo.getTaskId());

		verify(repo, timeout(3000).atLeastOnce()).save(argThat(doObj ->
				doObj.convertTo().getStatus() == RLTrainingStatus.READY));
		verifyNoInteractions(mktCenter);
	}

	@Test
	void loadDataFailureReturnsToCreated() throws Exception {
		RLTrainingTaskVO vo = service.createTask(dto());
		persistedAs(record(vo));
		when(dataManager.dataExists(eq(vo.getTaskId()), anyString())).thenReturn(false);
		when(mktCenter.getContract(eq(ChannelType.PLAYBACK), anyString()))
				.thenThrow(new IllegalStateException("合约不存在"));

		service.loadData(vo.getTaskId());

		verify(repo, timeout(3000).atLeastOnce()).save(argThat(doObj -> {
			RLTrainingTaskRecord r = doObj.convertTo();
			return r.getStatus() == RLTrainingStatus.CREATED
					&& r.getFailReason() != null
					&& r.getFailReason().contains("数据加载失败");
		}));
	}

	@Test
	void loadDataRejectsNonCreatedStatus() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord ready = record(vo);
		ready.setStatus(RLTrainingStatus.READY);
		persistedAs(ready);

		assertThrows(IllegalArgumentException.class, () -> service.loadData(vo.getTaskId()));
	}

	/* ---------- 启动训练 ---------- */

	@Test
	void startRequiresReady() {
		RLTrainingTaskVO vo = service.createTask(dto());
		persistedAs(record(vo));

		assertThrows(IllegalArgumentException.class, () -> service.start(vo.getTaskId()),
				"待加载状态不能直接开始训练");
	}

	@Test
	void startBuildsRunnerFromCsv() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord ready = record(vo);
		ready.setStatus(RLTrainingStatus.READY);
		persistedAs(ready);

		RLTrainingTaskVO started = service.start(vo.getTaskId());

		assertEquals(RLTrainingStatus.RUNNING, started.getStatus());
		assertEquals(1, service.built.size(), "启动时构建训练环境");
	}

	@Test
	void startBuildFailureMarksTaskFailed() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord ready = record(vo);
		ready.setStatus(RLTrainingStatus.READY);
		persistedAs(ready);
		service.runnerBuilder = _ -> {
			throw new IllegalStateException("预热时段 bar 数(10)不足策略要求的预热长度(60)");
		};

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> service.start(vo.getTaskId()));
		assertTrue(e.getMessage().contains("预热时段"), "应透出构建失败原因");
		verify(repo, atLeastOnce()).save(argThat(doObj ->
				doObj.convertTo().getStatus() == RLTrainingStatus.FAILED));
	}

	/* ---------- 删除任务 ---------- */

	@Test
	void deleteCreatedTaskRemovesRecordDataAndRollsBackFork() {
		RLTrainingTaskVO vo = service.createTask(dto());
		persistedAs(record(vo));

		service.deleteTask(vo.getTaskId());

		verify(repo).deleteById(vo.getTaskId());
		verify(dataManager).deleteTaskData(eq(vo.getTaskId()), anyString());
		assertFalse(Files.exists(modelStore.versionDir("dqn-demo", 1)), "未开始训练的任务删除时回收版本目录");
		assertEquals(0, modelStore.findModel("dqn-demo").orElseThrow().getLatestVersion());
	}

	@Test
	void deleteFinishedTaskKeepsVersion() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord finished = record(vo);
		finished.setStatus(RLTrainingStatus.FINISHED);
		persistedAs(finished);

		service.deleteTask(vo.getTaskId());

		verify(repo).deleteById(vo.getTaskId());
		assertTrue(Files.exists(modelStore.versionDir("dqn-demo", 1)), "已完成的任务删除时保留版本目录");
		assertEquals(1, modelStore.findModel("dqn-demo").orElseThrow().getLatestVersion());
	}

	@Test
	void deleteRejectsRunningOrPausedTask() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord running = record(vo);
		running.setStatus(RLTrainingStatus.RUNNING);
		persistedAs(running);

		assertThrows(IllegalArgumentException.class, () -> service.deleteTask(vo.getTaskId()));
		verify(repo, never()).deleteById(anyString());

		running.setStatus(RLTrainingStatus.PAUSED);
		persistedAs(running);
		assertThrows(IllegalArgumentException.class, () -> service.deleteTask(vo.getTaskId()));
	}

	@Test
	void deleteLoadingTaskCancelsLoadingAndRecyclesFork() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord loading = record(vo);
		loading.setStatus(RLTrainingStatus.LOADING);
		persistedAs(loading);

		service.deleteTask(vo.getTaskId());

		verify(repo).deleteById(vo.getTaskId());
		verify(dataManager).deleteTaskData(eq(vo.getTaskId()), anyString());
		assertFalse(Files.exists(modelStore.versionDir("dqn-demo", 1)), "LOADING 任务删除时回收未训练的版本");
		assertEquals(0, modelStore.findModel("dqn-demo").orElseThrow().getLatestVersion());
	}

	@Test
	void deleteCreatedTaskWithNewerVersionSucceedsNonLifo() {
		RLTrainingTaskVO taskA = service.createTask(dto());	// fork v1
		RLTrainingTaskVO taskB = service.createTask(dto());	// fork v2
		persistedAs(record(taskA));
		persistedAs(record(taskB));

		service.deleteTask(taskA.getTaskId());	// 先删旧任务（非 LIFO 顺序）

		verify(repo).deleteById(taskA.getTaskId());
		assertFalse(Files.exists(modelStore.versionDir("dqn-demo", 1)), "v1 目录应回收");
		assertTrue(Files.exists(modelStore.versionDir("dqn-demo", 2)), "v2 不受影响");
		assertEquals(2, modelStore.findModel("dqn-demo").orElseThrow().getLatestVersion(),
				"回收非最新版本时 latest_version 不变");
	}

	@Test
	void loadDataAbortsSilentlyWhenTaskDeletedMidway() {
		RLTrainingTaskVO vo = service.createTask(dto());
		persistedAs(record(vo));
		when(repo.existsById(vo.getTaskId())).thenReturn(false);	// 模拟任务在异步加载执行前已被删除
		clearInvocations(repo);

		service.loadData(vo.getTaskId());

		// 仅 loadData 同步落库一次（LOADING）；异步循环检测到删除后自清残留并安静退出，不再落库（防复活）
		verify(repo, timeout(1000).times(1)).save(any(RLTrainingTaskDO.class));
		verify(dataManager, timeout(1000)).deleteTaskData(eq(vo.getTaskId()), anyString());
	}

	@Test
	void loadingCancellationIsConsumedByLoadLoop() throws Exception {
		// 两个合约：首个合约拉取期间删除 LOADING 任务，加载循环在下一个合约边界消费取消标记
		RLTrainingTaskDTO twoContracts = dto();
		twoContracts.setContracts(List.of("rb0000@SHFE@FUTURES", "cu0000@SHFE@FUTURES"));
		RLTrainingTaskVO vo = service.createTask(twoContracts);
		String taskId = vo.getTaskId();
		persistedAs(record(vo));
		java.util.concurrent.CountDownLatch fetchStarted = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.CountDownLatch taskDeleted = new java.util.concurrent.CountDownLatch(1);
		when(dataManager.dataExists(eq(taskId), anyString())).thenReturn(false);
		when(mktCenter.getContract(eq(ChannelType.PLAYBACK), anyString())).thenReturn(mock(IContract.class));
		when(dataManager.fetchAndSave(any(), any(), eq(taskId), anyString())).thenAnswer(_ -> {
			fetchStarted.countDown();
			assertTrue(taskDeleted.await(5, java.util.concurrent.TimeUnit.SECONDS));
			return 100;
		});

		service.loadData(taskId);
		assertTrue(fetchStarted.await(5, java.util.concurrent.TimeUnit.SECONDS), "加载循环应已开始拉取首个合约");

		// 加载进行中删除任务：置取消标记 + 清数据目录 + 回收版本 + 删记录
		RLTrainingTaskRecord loading = record(vo);
		loading.setStatus(RLTrainingStatus.LOADING);
		persistedAs(loading);
		service.deleteTask(taskId);
		taskDeleted.countDown();

		verify(repo).deleteById(taskId);
		// deleteTask 清一次 + 加载循环消费取消标记兜底再清一次
		verify(dataManager, timeout(3000).atLeast(2)).deleteTaskData(eq(taskId), anyString());
		// 被取消的加载循环不得把任务推进到 READY
		verify(repo, after(500).never()).save(argThat(doObj ->
				doObj.convertTo().getStatus() == RLTrainingStatus.READY));
	}

	/* ---------- 生命周期 ---------- */

	@Test
	void lifecycleTransitionsWithoutRunner() {
		RLTrainingTaskVO vo = service.createTask(dto());
		String taskId = vo.getTaskId();
		RLTrainingTaskRecord ready = record(vo);
		ready.setStatus(RLTrainingStatus.READY);
		persistedAs(ready);

		RLTrainingTaskVO started = service.start(taskId);
		assertEquals(RLTrainingStatus.RUNNING, started.getStatus());
		persistedAs(record(started));	// 模拟落库后的最新状态

		RLTrainingTaskVO paused = service.pause(taskId);
		assertEquals(RLTrainingStatus.PAUSED, paused.getStatus());
		persistedAs(record(paused));

		RLTrainingTaskVO resumed = service.resume(taskId);
		assertEquals(RLTrainingStatus.RUNNING, resumed.getStatus());
		persistedAs(record(resumed));

		RLTrainingTaskVO stopped = service.stop(taskId);
		assertEquals(RLTrainingStatus.FINISHED, stopped.getStatus(), "无 runner 时停止直接完结");
		persistedAs(record(stopped));
		assertThrows(IllegalArgumentException.class, () -> service.pause(taskId), "已完结任务不能再暂停");
	}

	@Test
	void updateParamsValidatesKeysAndMerges() {
		RLTrainingTaskVO vo = service.createTask(dto());
		String taskId = vo.getTaskId();
		persistedAs(record(vo));

		RLTrainingTaskVO updated = service.updateParams(taskId, Map.of("gamma", 0.95));
		assertEquals(0.95, updated.getHyperParams().get("gamma"));
		assertEquals(0.001, updated.getHyperParams().get("lr"), "既有值保留");

		assertThrows(IllegalArgumentException.class,
				() -> service.updateParams(taskId, Map.of("badKey", 1.0)));
	}

	@Test
	void updateParamsRejectsTerminalTask() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord finished = record(vo);
		finished.setStatus(RLTrainingStatus.FINISHED);
		persistedAs(finished);

		assertThrows(IllegalArgumentException.class,
				() -> service.updateParams(vo.getTaskId(), Map.of("lr", 0.01)), "已完结任务不可调参");
	}

	@Test
	void updateParamsAllowsFailedTaskForRetry() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord failed = record(vo);
		failed.setStatus(RLTrainingStatus.FAILED);
		failed.setFailReason("Python 训练失败");
		persistedAs(failed);

		RLTrainingTaskVO updated = service.updateParams(vo.getTaskId(), Map.of("lr", 0.01));
		assertEquals(0.01, updated.getHyperParams().get("lr"), "失败任务修复问题后应可调整超参再重试");
	}

	@Test
	void updateParamsRejectsNonFiniteValue() {
		RLTrainingTaskVO vo = service.createTask(dto());
		persistedAs(record(vo));

		assertThrows(IllegalArgumentException.class,
				() -> service.updateParams(vo.getTaskId(), Map.of("lr", Double.NaN)), "NaN 为非法取值");
	}

	/* ---------- 暂停后继续（runner 重建分支） ---------- */

	@Test
	void resumeRebuildsRunnerWhenMissing() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord paused = record(vo);
		paused.setStatus(RLTrainingStatus.PAUSED);
		persistedAs(paused);
		RLTrainingRunner runner = mock(RLTrainingRunner.class);
		service.runnerBuilder = _ -> runner;

		RLTrainingTaskVO resumed = service.resume(vo.getTaskId());

		assertEquals(RLTrainingStatus.RUNNING, resumed.getStatus());
		assertEquals(1, service.built.size(), "服务重启后 runner 不在内存，resume 应重建训练环境");
		verify(runner).startAsync();
		verify(runner).resume();
	}

	@Test
	void resumeRebuildFailureKeepsTaskPaused() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord paused = record(vo);
		paused.setStatus(RLTrainingStatus.PAUSED);
		persistedAs(paused);
		service.runnerBuilder = _ -> {
			throw new IllegalStateException("CSV 数据损坏");
		};

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> service.resume(vo.getTaskId()));
		assertTrue(e.getMessage().contains("训练环境重建失败"), "应透出重建失败原因: " + e.getMessage());
		// 重建失败任务保持 PAUSED：不得落库 RUNNING
		verify(repo, never()).save(argThat(doObj ->
				doObj.convertTo().getStatus() == RLTrainingStatus.RUNNING));
		assertEquals(1, service.built.size(), "重建失败不应在 runnerMap 中残留半成品");
	}

	/* ---------- 重试失败任务 ---------- */

	@Test
	void retryRejectsNonFailedTask() {
		RLTrainingTaskVO vo = service.createTask(dto());
		persistedAs(record(vo));

		assertThrows(IllegalArgumentException.class, () -> service.retry(vo.getTaskId()), "非失败任务不可重试");
	}

	@Test
	void retryDiscardsFailedRoundShardsAndRestarts() throws Exception {
		RLTrainingTaskVO vo = service.createTask(dto());	// fork v1
		Path roundsDir = modelStore.roundsDir("dqn-demo", 1);
		Files.createDirectories(roundsDir);
		Files.writeString(roundsDir.resolve("replay-r0-c0.csv"), "hdr\n");
		Files.writeString(roundsDir.resolve("replay-r1-c0.csv"), "hdr\n");
		Files.writeString(roundsDir.resolve("replay-r1-c1.csv"), "hdr\n");
		Path ingestedFile = modelStore.versionDir("dqn-demo", 1).resolve("ingested.json");
		Files.writeString(ingestedFile, "[\"replay-r0-c0.csv\",\"replay-r1-c0.csv\"]");

		RLTrainingTaskRecord failed = record(vo);
		failed.setStatus(RLTrainingStatus.FAILED);
		failed.setFailReason("Python 训练失败，退出码 1");
		failed.setCurrentRound(1);
		persistedAs(failed);
		RLTrainingRunner startedRunner = mock(RLTrainingRunner.class);
		service.runnerBuilder = _ -> startedRunner;

		RLTrainingTaskVO retried = service.retry(vo.getTaskId());

		assertEquals(RLTrainingStatus.RUNNING, retried.getStatus());
		assertNull(retried.getFailReason(), "重试后应清空失败原因");
		assertTrue(Files.exists(roundsDir.resolve("replay-r0-c0.csv")), "已完成轮的分片保留");
		assertFalse(Files.exists(roundsDir.resolve("replay-r1-c0.csv")), "失败轮分片应清理");
		assertFalse(Files.exists(roundsDir.resolve("replay-r1-c1.csv")), "失败轮分片应清理");
		assertEquals("[\"replay-r0-c0.csv\"]", Files.readString(ingestedFile), "ingested 清单回滚失败轮条目");
		assertEquals(1, service.built.size(), "重试应重建训练环境");
		verify(startedRunner).startAsync();
	}

	@Test
	void retryWithoutShardsRestartsCleanly() throws Exception {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord failed = record(vo);
		failed.setStatus(RLTrainingStatus.FAILED);
		failed.setCurrentRound(0);
		persistedAs(failed);
		RLTrainingRunner startedRunner = mock(RLTrainingRunner.class);
		service.runnerBuilder = _ -> startedRunner;

		RLTrainingTaskVO retried = service.retry(vo.getTaskId());

		assertEquals(RLTrainingStatus.RUNNING, retried.getStatus());
		assertEquals(1, service.built.size());
		verify(startedRunner).startAsync();
	}

	@Test
	void retryBuildFailureKeepsTaskFailed() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord failed = record(vo);
		failed.setStatus(RLTrainingStatus.FAILED);
		failed.setFailReason("旧失败原因");
		persistedAs(failed);
		service.runnerBuilder = _ -> {
			throw new IllegalStateException("CSV 数据损坏");
		};

		assertThrows(IllegalStateException.class, () -> service.retry(vo.getTaskId()));
		verify(repo).save(argThat(doObj -> {
			RLTrainingTaskRecord saved = doObj.convertTo();
			return saved.getStatus() == RLTrainingStatus.FAILED && saved.getFailReason() != null;
		}));
	}

	/* ---------- buildRunner 真实静态校验 ---------- */

	/** 不截获 buildRunner 的原生服务（验证真实的预热/时段静态校验） */
	private RLTrainingService rawService() {
		RLTrainingService svc = new RLTrainingService();
		svc.repo = repo;
		svc.appCtx = appCtx;
		svc.emitterHolder = emitterHolder;
		svc.modelStore = modelStore;
		svc.trainExecutor = new StubTrainExecutor();
		svc.mktCenter = mktCenter;
		svc.dataManager = dataManager;
		return svc;
	}

	private RLTrainingTaskRecord readyRecord() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingTaskRecord r = record(vo);
		r.setStatus(RLTrainingStatus.READY);
		return r;
	}

	private static List<org.dromara.northstar.common.model.core.Bar> barsOn(String date, int count) {
		java.time.LocalDate day = java.time.LocalDate.parse(date);
		List<org.dromara.northstar.common.model.core.Bar> bars = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			bars.add(org.dromara.northstar.common.model.core.Bar.builder()
					.actionDay(day).actionTime(java.time.LocalTime.of(9, 0).plusMinutes(i))
					.actionTimestamp(i).build());
		}
		return bars;
	}

	@Test
	void buildRunnerRejectsInsufficientWarmupBars() throws Exception {
		RLTrainingTaskRecord ready = readyRecord();
		IContract ic = mock(IContract.class);
		when(ic.contract()).thenReturn(mock(org.dromara.northstar.common.model.core.Contract.class));
		when(mktCenter.getContract(eq(ChannelType.PLAYBACK), anyString())).thenReturn(ic);
		// 全部 bar 早于训练起始日：预热段仅 10 根，不足策略要求的 60 根
		when(dataManager.load(eq(ready.getTaskId()), any())).thenReturn(barsOn("2024-01-10", 10));

		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> rawService().buildRunner(ready));
		assertTrue(e.getMessage().contains("预热时段"), e.getMessage());
	}

	@Test
	void buildRunnerRejectsInvalidTimeSplit() throws Exception {
		RLTrainingTaskRecord ready = readyRecord();
		IContract ic = mock(IContract.class);
		when(ic.contract()).thenReturn(mock(org.dromara.northstar.common.model.core.Contract.class));
		when(mktCenter.getContract(eq(ChannelType.PLAYBACK), anyString())).thenReturn(ic);
		// 训练段 70 根（满足预热），但全部 bar 早于验证起始日：验证段为空
		List<org.dromara.northstar.common.model.core.Bar> bars = new ArrayList<>(barsOn("2024-01-15", 70));
		bars.addAll(barsOn("2024-02-15", 30));
		when(dataManager.load(eq(ready.getTaskId()), any())).thenReturn(bars);

		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> rawService().buildRunner(ready));
		assertTrue(e.getMessage().contains("时段划分无效"), e.getMessage());
	}

	/* ---------- 删除任务（runner 注册分支）与 resetAll ---------- */

	@Test
	void deleteFinishedTaskStopsRegisteredRunner() {
		RLTrainingTaskVO vo = service.createTask(dto());
		RLTrainingRunner runner = mock(RLTrainingRunner.class);
		when(runner.awaitTermination(any())).thenReturn(true);
		service.runnerBuilder = _ -> runner;
		RLTrainingTaskRecord ready = record(vo);
		ready.setStatus(RLTrainingStatus.READY);
		persistedAs(ready);
		service.start(vo.getTaskId());	// runner 注册进 runnerMap

		RLTrainingTaskRecord finished = record(vo);
		finished.setStatus(RLTrainingStatus.FINISHED);
		persistedAs(finished);

		service.deleteTask(vo.getTaskId());

		verify(runner).stop();
		verify(runner).awaitTermination(any());
		verify(repo).deleteById(vo.getTaskId());
		assertTrue(Files.exists(modelStore.versionDir("dqn-demo", 1)), "FINISHED 任务保留版本目录");
	}

	@Test
	void resetAllStopsRunnersAndCleansUntrainedVersions() {
		RLTrainingTaskVO taskA = service.createTask(dto());	// fork v1
		RLTrainingTaskVO taskB = service.createTask(dto());	// fork v2
		RLTrainingRunner runner = mock(RLTrainingRunner.class);
		when(runner.awaitTermination(any())).thenReturn(true);
		service.runnerBuilder = _ -> runner;
		RLTrainingTaskRecord readyA = record(taskA);
		readyA.setStatus(RLTrainingStatus.READY);
		persistedAs(readyA);
		service.start(taskA.getTaskId());	// 注册一个 runner（模拟重置时仍有运行痕迹）

		// 重置时库中记录：A 未训练（READY → 回收版本），B 已完结（FINISHED → 保留版本）
		RLTrainingTaskRecord finishedB = record(taskB);
		finishedB.setStatus(RLTrainingStatus.FINISHED);
		when(repo.findAll()).thenReturn(List.of(
				RLTrainingTaskDO.convertFrom(readyA), RLTrainingTaskDO.convertFrom(finishedB)));

		service.resetAll();

		verify(runner).stop();
		verify(runner).awaitTermination(any());
		verify(dataManager).deleteTaskData(eq(taskA.getTaskId()), anyString());
		verify(dataManager).deleteTaskData(eq(taskB.getTaskId()), anyString());
		assertFalse(Files.exists(modelStore.versionDir("dqn-demo", 1)), "未训练版本应回收");
		assertTrue(Files.exists(modelStore.versionDir("dqn-demo", 2)), "已训练版本应保留");
		assertEquals(2, modelStore.findModel("dqn-demo").orElseThrow().getLatestVersion());
		verify(repo).deleteAll();
	}

	@Test
	void startupRecoveryMarksRunningTaskPaused() {
		RLTrainingTaskRecord running = record(service.createTask(dto()));
		running.setStatus(RLTrainingStatus.RUNNING);
		when(repo.findAll()).thenReturn(List.of(RLTrainingTaskDO.convertFrom(running)));

		service.run();

		verify(repo).save(argThat(doObj -> {
			RLTrainingTaskRecord r = doObj.convertTo();
			return r.getStatus() == RLTrainingStatus.PAUSED;
		}));
	}

	@Test
	void startupRecoveryMarksLoadingTaskCreated() {
		RLTrainingTaskRecord loading = record(service.createTask(dto()));
		loading.setStatus(RLTrainingStatus.LOADING);
		when(repo.findAll()).thenReturn(List.of(RLTrainingTaskDO.convertFrom(loading)));

		service.run();

		verify(repo).save(argThat(doObj -> {
			RLTrainingTaskRecord r = doObj.convertTo();
			return r.getStatus() == RLTrainingStatus.CREATED
					&& r.getFailReason() != null
					&& r.getFailReason().contains("重启");
		}));
	}

	/* ---------- 工具方法 ---------- */

	private RLTrainingTaskRecord record(RLTrainingTaskVO vo) {
		return RLTrainingTaskRecord.builder()
				.taskId(vo.getTaskId())
				.strategyClass(vo.getStrategyClass())
				.strategyName(vo.getStrategyName())
				.modelId(vo.getModelId())
				.modelName(vo.getModelName())
				.algo(vo.getAlgo())
				.baseVersion(vo.getBaseVersion())
				.targetVersion(vo.getTargetVersion())
				.contracts(vo.getContracts())
				.warmupStartDate(vo.getWarmupStartDate())
				.trainStartDate(vo.getTrainStartDate())
				.validationStartDate(vo.getValidationStartDate())
				.totalRounds(vo.getTotalRounds())
				.episodesPerRound(vo.getEpisodesPerRound())
				.initBalance(vo.getInitBalance())
				.maxHoldingBars(vo.getMaxHoldingBars())
				.ruinThreshold(vo.getRuinThreshold())
				.maxEpisodeBars(vo.getMaxEpisodeBars())
				.defaultVolume(vo.getDefaultVolume())
				.hyperParams(new HashMap<>(vo.getHyperParams()))
				.status(vo.getStatus())
				.currentRound(vo.getCurrentRound())
				.createdAt(vo.getCreatedAt())
				.seed(1)
				.metrics(new ArrayList<>())
				.build();
	}

	private void persistedAs(RLTrainingTaskRecord record) {
		when(repo.findById(record.getTaskId()))
				.thenReturn(java.util.Optional.of(RLTrainingTaskDO.convertFrom(record)));
		when(repo.existsById(record.getTaskId())).thenReturn(true);
	}
}
