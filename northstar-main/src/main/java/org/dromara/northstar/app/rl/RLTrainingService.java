package org.dromara.northstar.app.rl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

import com.alibaba.fastjson2.JSON;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.rl.RLModelVO;
import org.dromara.northstar.common.model.rl.RLRoundMetrics;
import org.dromara.northstar.common.model.rl.RLStrategyVO;
import org.dromara.northstar.common.model.rl.RLTrainingStatus;
import org.dromara.northstar.common.model.rl.RLTrainingTaskDTO;
import org.dromara.northstar.common.model.rl.RLTrainingTaskDetailVO;
import org.dromara.northstar.common.model.rl.RLTrainingTaskVO;
import org.dromara.northstar.data.jdbc.RLTrainingTaskRepository;
import org.dromara.northstar.data.jdbc.entity.RLTrainingTaskDO;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.rl.agent.RLAgent;
import org.dromara.northstar.rl.model.RLModelMetadata;
import org.dromara.northstar.rl.model.RLModelStore;
import org.dromara.northstar.rl.model.RLVersionMetadata;
import org.dromara.northstar.rl.training.RLAlgorithms;
import org.dromara.northstar.rl.training.RLTrainExecutor;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.rl.RLStrategy;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

/**
 * RL 训练服务：任务生命周期管理（创建/加载数据/启动训练/查询/调参/暂停/继续/停止）+ 模型与策略目录服务。
 *
 * 任务模式参照 StrategyExplorerService：内存 runner 注册表 + JPA 持久化 + 启动恢复。
 * 生命周期三段式：创建（CREATED）→ 数据加载（LOADING→READY，行情落地 CSV，已存在则跳过）
 * → 启动训练（RUNNING，Runner 只读 CSV，全程零数据源 I/O）。
 * 训练进度的每轮指标通过 SSE（事件名 RL_TRAINING）推送前端。
 */
@Slf4j
@Service
public class RLTrainingService implements CommandLineRunner {

	/** SSE 事件名 */
	public static final String SSE_EVENT = "RL_TRAINING";

	@Autowired
	RLTrainingTaskRepository repo;
	@Autowired
	ApplicationContext appCtx;
	@Autowired
	MarketCenter mktCenter;
	@Autowired
	EventEmitterHolder emitterHolder;
	@Autowired
	RLModelStore modelStore;
	@Autowired
	RLTrainExecutor trainExecutor;
	@Autowired
	RLTrainingDataManager dataManager;

	/** 多任务串行队列（v1 不做并发训练资源仲裁） */
	private final Semaphore trainLock = new Semaphore(1);
	private final Map<String, RLTrainingRunner> runnerMap = new ConcurrentHashMap<>();
	/** 数据加载取消标记：删除 LOADING 任务时置位，异步加载循环在合约边界消费并自清残留文件 */
	private final Set<String> loadCancelRequests = ConcurrentHashMap.newKeySet();

	/* ---------------- 目录服务 ---------------- */

	/** RL 策略列表（仅 RLStrategy 实现类） */
	public List<RLStrategyVO> rlStrategies() {
		return appCtx.getBeansWithAnnotation(StrategicComponent.class).values().stream()
				.filter(RLStrategy.class::isInstance)
				.map(bean -> {
					StrategicComponent anno = bean.getClass().getAnnotation(StrategicComponent.class);
					return RLStrategyVO.builder()
							.name(anno.value())
							.className(bean.getClass().getName())
							.build();
				})
				.toList();
	}

	/** 模型列表（含 hyperParamFields、versionDetails、network） */
	public List<RLModelVO> models() {
		return modelStore.listModels().stream()
				.map(meta -> RLModelVO.builder()
						.modelId(modelIdOf(meta))
						.name(meta.getName())
						.algo(meta.getAlgo())
						.strategyClass(meta.getStrategyClass() == null ? "" : meta.getStrategyClass())
						.network(latestVersionNetwork(modelIdOf(meta), meta.getLatestVersion()))
						.hyperParamFields(new ArrayList<>(RLAlgorithms.hyperParamsOf(meta.getAlgo()).getMetaInfo()))
						.versionDetails(modelStore.listVersions(modelIdOf(meta)).stream()
								.map(v -> RLModelVO.VersionMetaVO.builder()
										.version(v.getVersion())
										.hyperParams(v.getTraining().getHyperParams())
										.build())
								.toList())
						.latestVersion(meta.getLatestVersion())
						.build())
				.toList();
	}

	/**
	 * 模型网络结构：读取最新版本的版本级 metadata 中 Python 回写的 network 字段（不透明透传）。
	 * 无已训练版本或 Python 尚未回写时为 null。
	 */
	private RLModelVO.NetworkVO latestVersionNetwork(String modelId, int latestVersion) {
		if (latestVersion <= 0) {
			return null;
		}
		return modelStore.findVersion(modelId, latestVersion)
				.map(RLVersionMetadata::getNetwork)
				.filter(Objects::nonNull)
				.map(network -> {
					Object hidden = network.get("hidden");
					List<Integer> hiddenLayers = hidden instanceof List<?> list
							? list.stream().map(n -> ((Number) n).intValue()).toList()
							: null;
					return RLModelVO.NetworkVO.builder()
							.hidden(hiddenLayers)
							.activation(network.get("activation") instanceof String s ? s : null)
							.build();
				})
				.orElse(null);
	}

	/* ---------------- 任务生命周期 ---------------- */

	public synchronized RLTrainingTaskVO createTask(RLTrainingTaskDTO dto) {
		Assert.hasText(dto.getStrategyClass(), "策略不能为空");
		Assert.hasText(dto.getModelId(), "模型不能为空");
		Assert.isTrue(dto.getModelId().matches("[\\w\\-.]+") && !dto.getModelId().matches("\\.{1,2}"),
				"模型 ID 含非法字符（仅允许字母数字、下划线、中划线、点，且不能为 . 或 ..）");
		Assert.notEmpty(dto.getContracts(), "合约不能为空");
		Assert.isTrue(dto.getTotalRounds() > 0, "总轮数必须为正数");
		Assert.isTrue(dto.getEpisodesPerRound() > 0, "每轮局数必须为正数");
		Assert.notNull(dto.getInitBalance(), "沙箱初始资金不能为空");
		Assert.isTrue(dto.getInitBalance() > 0, "沙箱初始资金必须为正数");
		Assert.notNull(dto.getMaxHoldingBars(), "决策点超时强触发 bar 数不能为空");
		Assert.isTrue(dto.getMaxHoldingBars() >= 1, "决策点超时强触发 bar 数至少为 1");
		Assert.notNull(dto.getRuinThreshold(), "权益击穿阈值不能为空");
		Assert.isTrue(dto.getRuinThreshold() > 0 && dto.getRuinThreshold() < 1,
				"权益击穿阈值必须在 (0, 1) 区间内");
		Assert.notNull(dto.getMaxEpisodeBars(), "单局最长 bar 数不能为空");
		Assert.isTrue(dto.getMaxEpisodeBars() >= 1, "单局最长 bar 数至少为 1");

		// 三个起始日期严格递增：预热=[预热起始, 训练起始)、训练=[训练起始, 验证起始)、验证=[验证起始, 数据末日]
		LocalDate warmupStart = LocalDate.parse(dto.getWarmupStartDate(), DateTimeConstant.D_FORMAT_FORMATTER);
		LocalDate trainStart = LocalDate.parse(dto.getTrainStartDate(), DateTimeConstant.D_FORMAT_FORMATTER);
		LocalDate validationStart = LocalDate.parse(dto.getValidationStartDate(), DateTimeConstant.D_FORMAT_FORMATTER);
		Assert.isTrue(warmupStart.isBefore(trainStart) && trainStart.isBefore(validationStart),
				"预热/训练/验证起始日必须严格递增");

		RLStrategy strategy = findRLStrategy(dto.getStrategyClass());
		if (!dto.getModelId().equals(strategy.modelName())) {
			throw new IllegalArgumentException(String.format(
					"模型与策略不匹配：策略 %s 绑定的模型名为 %s，与请求模型 %s 不一致",
					dto.getStrategyClass(), strategy.modelName(), dto.getModelId()));
		}
		// 模型级 metadata 是可重建的传输载荷：缺失/陈旧一律由策略现场 spec 引导重建（syncModel），
		// 不再要求 metadata 预置，也不因 metadata 缺失/陈旧而失败
		RLModelMetadata model = modelStore.syncModel(strategy);
		Assert.isTrue(dto.getStrategyClass().equals(model.getStrategyClass()),
				"模型与策略不匹配：模型绑定 " + model.getStrategyClass());

		// 超参数：字段清单来自模型的 Hyperparameter 定义，值 = 默认值 + 表单覆盖
		Map<String, Double> hyperParams = RLAlgorithms.hyperParamDefaults(model.getAlgo());
		if (dto.getHyperParams() != null) {
			dto.getHyperParams().forEach((k, v) -> {
				if (!hyperParams.containsKey(k)) {
					throw new IllegalArgumentException("模型 " + model.getAlgo() + " 不支持超参数: " + k);
				}
				hyperParams.put(k, v);
			});
		}

		// fork 新版本（双重校验 + 权重继承 / 显式降级）
		// 采样种子限定在 [0, 2^32-1]：Python 端 numpy 播种只允许该范围，超出会直接报错
		long seed = UUID.randomUUID().getLeastSignificantBits() & 0xFFFFFFFFL;
		RLVersionMetadata.TrainingInfo training = new RLVersionMetadata.TrainingInfo();
		training.setRounds(dto.getTotalRounds());
		training.setWarmupStartDate(dto.getWarmupStartDate());
		training.setTrainStartDate(dto.getTrainStartDate());
		training.setValidationStartDate(dto.getValidationStartDate());
		training.setContracts(dto.getContracts());
		training.setSeed(seed);
		// 触发条件摘要：影响转移语义但不进 specHash，记入版本 metadata 供复盘
		training.setTriggerSummary(String.format("策略=%s, 决策点超时强触发=%dbar, 权益击穿阈值=%s, 单局最长=%dbar",
				dto.getStrategyClass(), dto.getMaxHoldingBars(), dto.getRuinThreshold(), dto.getMaxEpisodeBars()));
		boolean inheritWeights = dto.getInheritWeights() != null ? dto.getInheritWeights() : dto.getBaseVersion() > 0;
		RLVersionMetadata version = modelStore.forkVersion(
				strategy, dto.getBaseVersion(), inheritWeights, hyperParams, training);

		RLTrainingTaskRecord record = RLTrainingTaskRecord.builder()
				.taskId(UUID.randomUUID().toString())
				.strategyClass(dto.getStrategyClass())
				.strategyName(strategy.name())
				.modelId(dto.getModelId())
				.modelName(model.getName())
				.algo(model.getAlgo())
				.baseVersion(dto.getBaseVersion())
				.targetVersion(version.getVersion())
				.contracts(dto.getContracts())
				.warmupStartDate(dto.getWarmupStartDate())
				.trainStartDate(dto.getTrainStartDate())
				.validationStartDate(dto.getValidationStartDate())
				.totalRounds(dto.getTotalRounds())
				.episodesPerRound(dto.getEpisodesPerRound())
				.initBalance(dto.getInitBalance())
				.maxHoldingBars(dto.getMaxHoldingBars())
				.ruinThreshold(dto.getRuinThreshold())
				.maxEpisodeBars(dto.getMaxEpisodeBars())
				.hyperParams(hyperParams)
				.status(RLTrainingStatus.CREATED)
				.currentRound(0)
				.createdAt(System.currentTimeMillis())
				.seed(seed)
				.metrics(new ArrayList<>())
				.build();
		try {
			// 创建只做参数校验与版本 fork，不加载数据、不启动训练；落库失败回收 fork 的版本
			repo.save(RLTrainingTaskDO.convertFrom(record));
			return toVO(record);
		} catch (Exception e) {
			// 创建失败：回收刚 fork 的版本目录并回退 latest_version，不留半提交状态；
			// 回收失败不掩盖原始失败，作为 suppressed 附加到原异常，便于排查版本目录泄漏
			try {
				recycleForkedVersion(dto.getModelId(), version.getVersion());
			} catch (Exception rollbackEx) {
				log.warn("回滚模型 {} 版本 v{} 失败: {}", dto.getModelId(), version.getVersion(), rollbackEx.getMessage());
				e.addSuppressed(rollbackEx);
			}
			throw e instanceof RuntimeException re
					? re
					: new IllegalStateException("训练任务创建失败：" + e.getMessage(), e);
		}
	}

	/**
	 * 异步加载训练数据：逐合约从数据源拉取并落地 CSV；已存在的合约文件直接跳过（断点续传）。
	 * 全部就绪后任务进入 READY；任一合约失败回到 CREATED 并记录原因，可重试。
	 */
	public synchronized RLTrainingTaskVO loadData(String taskId) {
		RLTrainingTaskRecord record = findRecord(taskId);
		Assert.isTrue(record.getStatus() == RLTrainingStatus.CREATED, "仅待加载状态的任务可以加载数据");
		record.setStatus(RLTrainingStatus.LOADING);
		record.setFailReason(null);
		record.setDataLoadedContracts(0);
		record.setDataLoadedBars(0);
		record.setDataCurrentContract(null);
		repo.save(RLTrainingTaskDO.convertFrom(record));
		notify(record);
		CompletableFuture.runAsync(() -> doLoadData(record));
		return toVO(record);
	}

	private void doLoadData(RLTrainingTaskRecord record) {
		try {
			LocalDate warmupStart = LocalDate.parse(record.getWarmupStartDate(), DateTimeConstant.D_FORMAT_FORMATTER);
			for (String unifiedSymbol : record.getContracts()) {
				if (loadCancelled(record)) {
					return;
				}
				record.setDataCurrentContract(unifiedSymbol);
				if (dataManager.dataExists(record.getTaskId(), unifiedSymbol)) {
					log.info("RL 任务 {} 合约 {} 数据文件已存在，跳过加载", record.displayName(), unifiedSymbol);
				} else {
					IContract ic = mktCenter.getContract(ChannelType.PLAYBACK, unifiedSymbol);
					int bars = dataManager.fetchAndSave(ic, warmupStart, record.getTaskId(), record.displayName());
					record.setDataLoadedBars(record.getDataLoadedBars() + bars);
				}
				record.setDataLoadedContracts(record.getDataLoadedContracts() + 1);
				persistQuietly(record);
				notify(record);
			}
			if (loadCancelled(record)) {
				return;
			}
			record.setStatus(RLTrainingStatus.READY);
			record.setDataCurrentContract(null);
		} catch (Exception e) {
			log.error("RL 任务 {} 数据加载失败", record.displayName(), e);
			record.setStatus(RLTrainingStatus.CREATED);
			record.setFailReason("数据加载失败：" + e.getMessage());
		}
		persistQuietly(record);
		notify(record);
	}

	/**
	 * 加载是否已被取消（任务在 LOADING 期间被删除）：消费取消标记并自清残留文件——
	 * 删除方清理数据目录后，加载循环可能又写入新文件，此处兜底再清一次，随后安静退出不落库。
	 */
	private boolean loadCancelled(RLTrainingTaskRecord record) {
		String taskId = record.getTaskId();
		boolean cancelled = loadCancelRequests.remove(taskId);
		if (cancelled || !repo.existsById(taskId)) {
			log.info("RL 任务 {} 数据加载被取消（任务已删除），清理残留文件后退出", record.displayName());
			try {
				dataManager.deleteTaskData(taskId, record.displayName());
			} catch (Exception e) {
				log.warn("清理已删除 RL 任务 {} 的残留数据失败: {}", record.displayName(), e.getMessage());
			}
			return true;
		}
		return false;
	}

	/**
	 * 启动训练：同步构建 Runner（读 CSV + 预热/时段静态校验），校验失败任务置 FAILED 并透出原因。
	 */
	public synchronized RLTrainingTaskVO start(String taskId) {
		RLTrainingTaskRecord record = findRecord(taskId);
		Assert.isTrue(record.getStatus() == RLTrainingStatus.READY, "仅数据就绪的任务可以开始训练");
		try {
			RLTrainingRunner runner = buildRunner(record);
			runnerMap.put(record.getTaskId(), runner);
			record.setStatus(RLTrainingStatus.RUNNING);
			repo.save(RLTrainingTaskDO.convertFrom(record));
			notify(record);
			runner.startAsync();
			return toVO(record);
		} catch (Exception e) {
			record.setStatus(RLTrainingStatus.FAILED);
			record.setFailReason(e.getMessage());
			persistQuietly(record);
			notify(record);
			throw e instanceof RuntimeException re
					? re
					: new IllegalStateException("训练启动失败：" + e.getMessage(), e);
		}
	}

	public List<RLTrainingTaskVO> listTasks() {
		List<RLTrainingTaskRecord> records = new ArrayList<>();
		repo.findAll().forEach(doObj -> records.add(doObj.convertTo()));
		return records.stream()
				.sorted(Comparator.comparingLong(RLTrainingTaskRecord::getCreatedAt).reversed())
				.map(this::toVO)
				.toList();
	}

	public RLTrainingTaskDetailVO detail(String taskId) {
		RLTrainingTaskRecord record = findRecord(taskId);
		return RLTrainingTaskDetailVO.builder()
				.task(toVO(record))
				.metrics(record.getMetrics())
				.build();
	}

	/** 动态调整超参数（下一轮生效）；FINISHED 任务拒绝调整，FAILED 任务可在修复问题后重试前调整 */
	public synchronized RLTrainingTaskVO updateParams(String taskId, Map<String, Double> params) {
		RLTrainingTaskRecord record = findRecord(taskId);
		Assert.isTrue(record.getStatus() != RLTrainingStatus.FINISHED, "已完结的任务不可调整超参数");
		Assert.notEmpty(params, "超参数不能为空");
		RLModelMetadata model = modelStore.syncModel(findRLStrategy(record.getStrategyClass()));
		Map<String, Double> defaults = RLAlgorithms.hyperParamDefaults(model.getAlgo());
		Map<String, Double> merged = new HashMap<>(record.getHyperParams());
		params.forEach((k, v) -> {
			if (!defaults.containsKey(k)) {
				throw new IllegalArgumentException("模型 " + model.getAlgo() + " 不支持超参数: " + k);
			}
			if (v == null || !Double.isFinite(v)) {
				throw new IllegalArgumentException("超参数 " + k + " 取值非法（须为有限数值）: " + v);
			}
			merged.put(k, v);
		});
		record.setHyperParams(merged);
		repo.save(RLTrainingTaskDO.convertFrom(record));
		return toVO(record);
	}

	public RLTrainingTaskVO pause(String taskId) {
		RLTrainingRunner runner = runnerMap.get(taskId);
		RLTrainingTaskRecord record = findRecord(taskId);
		Assert.isTrue(record.getStatus() == RLTrainingStatus.RUNNING, "仅运行中的任务可以暂停");
		if (runner != null) {
			runner.pause();
		}
		record.setStatus(RLTrainingStatus.PAUSED);
		repo.save(RLTrainingTaskDO.convertFrom(record));
		notify(record);
		return toVO(record);
	}

	public synchronized RLTrainingTaskVO resume(String taskId) {
		RLTrainingTaskRecord record = findRecord(taskId);
		Assert.isTrue(record.getStatus() == RLTrainingStatus.PAUSED, "仅已暂停的任务可以继续");
		RLTrainingRunner runner = runnerMap.computeIfAbsent(taskId, _ -> {
			try {
				// 服务重启后 runner 不在内存：同步重建训练环境后继续（checkpoint 与经验分片均持久化在版本目录）；
				// 重建失败直接抛给前端，任务保持暂停状态
				RLTrainingRunner rebuilt = buildRunner(record);
				rebuilt.startAsync();
				return rebuilt;
			} catch (Exception e) {
				throw new IllegalStateException("训练环境重建失败：" + e.getMessage(), e);
			}
		});
		record.setStatus(RLTrainingStatus.RUNNING);
		runner.resume();
		repo.save(RLTrainingTaskDO.convertFrom(record));
		notify(record);
		return toVO(record);
	}

	/**
	 * 重试失败任务：清理失败轮的经验分片（含 ingested 清单回滚）后从当前轮断点续训。
	 *
	 * checkpoint/buffer 由 Python 侧原子写保证一致，失败轮不会产生半提交的训练状态；
	 * 分片回滚后重采样该轮，采样序列由 (seed, round) 确定，重试可复现。
	 * 若失败前 buffer 已持久化该轮样本（buffer 已存但 ingested 未更新的崩溃窗口），
	 * 重试后该轮样本会重复入库一次——与进程崩溃的 at-least-once 语义一致，不影响正确性。
	 */
	public synchronized RLTrainingTaskVO retry(String taskId) {
		RLTrainingTaskRecord record = findRecord(taskId);
		Assert.isTrue(record.getStatus() == RLTrainingStatus.FAILED, "仅失败的任务可以重试");
		discardFailedRoundShards(record);
		try {
			RLTrainingRunner runner = buildRunner(record);
			runnerMap.put(record.getTaskId(), runner);
			record.setStatus(RLTrainingStatus.RUNNING);
			record.setFailReason(null);
			repo.save(RLTrainingTaskDO.convertFrom(record));
			notify(record);
			runner.startAsync();
			return toVO(record);
		} catch (Exception e) {
			record.setStatus(RLTrainingStatus.FAILED);
			record.setFailReason(e.getMessage());
			persistQuietly(record);
			notify(record);
			throw e instanceof RuntimeException re
					? re
					: new IllegalStateException("训练重试启动失败：" + e.getMessage(), e);
		}
	}

	/**
	 * 丢弃失败轮（currentRound）的全部经验分片，并把对应文件名从 ingested.json 移除，
	 * 使重试重采样该轮时新分片可被 Python 增量读取（分片文件名按 轮次×合约 命名，不清理会被
	 * ExperienceStore 的 APPEND 语义追加重复行、且已 ingested 的旧文件名会遮蔽新分片）。
	 */
	private void discardFailedRoundShards(RLTrainingTaskRecord record) {
		Path roundsDir = modelStore.roundsDir(record.getModelId(), record.getTargetVersion());
		if (!Files.isDirectory(roundsDir)) {
			return;
		}
		String shardPrefix = "replay-r%d-c".formatted(record.getCurrentRound());
		List<String> discarded = new ArrayList<>();
		try (Stream<Path> stream = Files.list(roundsDir)) {
			for (Path shard : stream.filter(p -> p.getFileName().toString().startsWith(shardPrefix)).toList()) {
				Files.deleteIfExists(shard);
				discarded.add(shard.getFileName().toString());
			}
		} catch (IOException e) {
			throw new UncheckedIOException("清理失败轮经验分片失败: " + roundsDir, e);
		}
		if (discarded.isEmpty()) {
			return;
		}
		Path ingestedFile = modelStore.versionDir(record.getModelId(), record.getTargetVersion()).resolve("ingested.json");
		if (!Files.exists(ingestedFile)) {
			return;
		}
		try {
			List<String> ingested = new ArrayList<>(JSON.parseArray(Files.readString(ingestedFile), String.class));
			if (ingested.removeAll(discarded)) {
				// 先写临时文件再原子替换，与 Python 侧 _atomic_write_text 语义一致
				Path tmp = ingestedFile.resolveSibling(ingestedFile.getFileName() + ".tmp");
				Files.writeString(tmp, JSON.toJSONString(ingested));
				try {
					Files.move(tmp, ingestedFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
					Files.move(tmp, ingestedFile, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException("回滚 ingested 清单失败: " + ingestedFile, e);
		}
	}

	/**
	 * 删除任务：运行中/已暂停需先停止；LOADING 任务通过取消标记协作式终止加载循环。
	 * 未开始训练的任务（CREATED/LOADING/READY）同时回收 fork 的版本（与创建失败语义一致）；
	 * 已训练/失败的任务保留版本目录（训练成果可能被模组绑定或被后续任务引用）。
	 *
	 * 操作顺序保证失败可重入：先等待训练循环退出 → 删数据（失败则中止，任务保留可重试）
	 * → 回收版本（失败同样中止并抛出，不留下"记录已删但版本/数据残留"的半提交状态）→ 最后删记录。
	 */
	public synchronized void deleteTask(String taskId) {
		RLTrainingTaskRecord record = findRecord(taskId);
		Assert.isTrue(record.getStatus() != RLTrainingStatus.RUNNING && record.getStatus() != RLTrainingStatus.PAUSED,
				"运行中/已暂停的任务请先停止再删除");
		RLTrainingRunner runner = runnerMap.remove(taskId);
		if (runner != null) {
			runner.stop();
			if (!runner.awaitTermination(Duration.ofSeconds(10))) {
				// 训练循环仍在轮内（Python 训练可能耗时分钟级）：继续删除，滞后落库由 persistQuietly 抑制
				log.warn("RL 任务 {} 训练循环未在 10s 内退出，继续删除", record.displayName());
			}
		}
		if (record.getStatus() == RLTrainingStatus.LOADING) {
			loadCancelRequests.add(taskId);
		}
		dataManager.deleteTaskData(taskId, record.displayName());
		if (record.getStatus() == RLTrainingStatus.CREATED || record.getStatus() == RLTrainingStatus.LOADING
				|| record.getStatus() == RLTrainingStatus.READY) {
			recycleForkedVersion(record.getModelId(), record.getTargetVersion());
		}
		repo.deleteById(taskId);
	}

	public RLTrainingTaskVO stop(String taskId) {
		RLTrainingTaskRecord record = findRecord(taskId);
		Assert.isTrue(record.getStatus() == RLTrainingStatus.RUNNING || record.getStatus() == RLTrainingStatus.PAUSED,
				"仅运行中/已暂停的任务可以停止");
		RLTrainingRunner runner = runnerMap.get(taskId);
		if (runner != null) {
			runner.stop();	// 通知执行循环在轮边界停止；当轮指标仍会结算入册
		}
		// 立即反馈停止结果：执行循环在轮边界退出后保持 FINISHED，不再变更状态
		record.setStatus(RLTrainingStatus.FINISHED);
		repo.save(RLTrainingTaskDO.convertFrom(record));
		notify(record);
		return toVO(record);
	}

	/* ---------------- 启动恢复 ---------------- */

	/** 清空全部训练任务（仅 e2e/dev 环境重置用）；等待训练循环退出后再清理，避免残留线程干扰后续操作 */
	public synchronized void resetAll() {
		runnerMap.values().forEach(RLTrainingRunner::stop);
		runnerMap.values().forEach(runner -> {
			if (!runner.awaitTermination(Duration.ofSeconds(10))) {
				log.warn("RL 任务 {} 训练循环未在重置等待窗口内退出（滞后落库将被抑制）", runner.displayName());
			}
		});
		runnerMap.clear();
		List<RLTrainingTaskRecord> records = new ArrayList<>();
		repo.findAll().forEach(doObj -> records.add(doObj.convertTo()));
		records.forEach(record -> {
			try {
				dataManager.deleteTaskData(record.getTaskId(), record.displayName());
				if (record.getStatus() == RLTrainingStatus.CREATED || record.getStatus() == RLTrainingStatus.LOADING
						|| record.getStatus() == RLTrainingStatus.READY) {
					recycleForkedVersion(record.getModelId(), record.getTargetVersion());
				}
			} catch (Exception e) {
				log.warn("重置清理 RL 任务 {} 残留资源失败: {}", record.displayName(), e.getMessage());
			}
		});
		repo.deleteAll();
	}

	@Override
	public void run(String... args) {
		// 服务重启后训练环境（内存态）无法恢复：RUNNING 任务标记为 PAUSED，等待人工继续（resume 时重建 runner）；
		// LOADING 任务标记回 CREATED，重新加载时按 CSV 文件存在性断点续传
		repo.findAll().forEach(doObj -> {
			RLTrainingTaskRecord record = doObj.convertTo();
			if (record.getStatus() == RLTrainingStatus.RUNNING) {
				record.setStatus(RLTrainingStatus.PAUSED);
				repo.save(RLTrainingTaskDO.convertFrom(record));
			} else if (record.getStatus() == RLTrainingStatus.LOADING) {
				record.setStatus(RLTrainingStatus.CREATED);
				record.setFailReason("服务重启导致数据加载中断，请重新加载数据");
				repo.save(RLTrainingTaskDO.convertFrom(record));
			}
		});
	}

	/* ---------------- 内部实现 ---------------- */

	/**
	 * 回收 fork 的版本（不吞异常）：删除版本目录并回退 latest_version。
	 * deleteTask 依赖抛出中止删记录以保证可重试；createTask/resetAll 在调用点自行兜底。
	 */
	private void recycleForkedVersion(String modelId, int version) {
		modelStore.deleteVersion(modelId, version);
	}

	/** 构建 Runner：从本地 CSV 读取各合约全时段数据并缓存于各环境（之后所有 episode 零 I/O） */
	RLTrainingRunner buildRunner(RLTrainingTaskRecord record) throws Exception {
		RLStrategy strategy = findRLStrategy(record.getStrategyClass());
		modelStore.syncModel(strategy);	// metadata 可重建：启动训练前确保模型级 metadata 与现场 spec 一致
		LocalDate trainStart = LocalDate.parse(record.getTrainStartDate(), DateTimeConstant.D_FORMAT_FORMATTER);
		LocalDate validationStart = LocalDate.parse(record.getValidationStartDate(), DateTimeConstant.D_FORMAT_FORMATTER);
		int warmupBars = strategy.rlWarmupBars();

		List<TradingEnvironment> envs = new ArrayList<>();
		int idx = 0;
		for (String unifiedSymbol : record.getContracts()) {
			IContract ic = mktCenter.getContract(ChannelType.PLAYBACK, unifiedSymbol);
			List<Bar> bars = dataManager.load(record.getTaskId(), ic.contract());
			int trainStartIdx = lowerBound(bars, trainStart);
			int validationStartIdx = lowerBound(bars, validationStart);
			if (trainStartIdx < warmupBars) {
				throw new IllegalStateException(String.format(
						"合约 %s 预热时段 bar 数(%d)不足策略要求的预热长度(%d)，请提前预热起始日", unifiedSymbol, trainStartIdx, warmupBars));
			}
			if (validationStartIdx <= trainStartIdx || validationStartIdx >= bars.size()) {
				throw new IllegalStateException(String.format(
						"合约 %s 训练/验证时段划分无效：训练段[%d, %d)，验证段[%d, %d)",
						unifiedSymbol, trainStartIdx, validationStartIdx, validationStartIdx, bars.size()));
			}
			RLAgent agent = new RLAgent(modelStore.modelDir(record.getModelId()), record.getTargetVersion());
			envs.add(new TradingEnvironment(ic, bars, trainStartIdx, validationStartIdx,
					strategy, agent, idx++, "rl-" + record.getTaskId().substring(0, 8),
					new TradingEnvironment.RLEnvConfig(record.getInitBalance(), warmupBars,
							record.getMaxHoldingBars(), record.getRuinThreshold(), record.getMaxEpisodeBars())));
		}
		return new RLTrainingRunner(record, envs, modelStore, trainExecutor, trainLock,
				() -> findRecord(record.getTaskId()), this::persistQuietly, this::notify);
	}

	private static int lowerBound(List<Bar> bars, LocalDate date) {
		int lo = 0, hi = bars.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (bars.get(mid).actionDay().isBefore(date)) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo;
	}

	private RLStrategy findRLStrategy(String strategyClass) {
		return appCtx.getBeansWithAnnotation(StrategicComponent.class).values().stream()
				.filter(bean -> bean.getClass().getName().equals(strategyClass))
				.filter(RLStrategy.class::isInstance)
				.map(RLStrategy.class::cast)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"RL 策略不存在或未实现 RLStrategy 接口: " + strategyClass));
	}

	private String modelIdOf(RLModelMetadata meta) {
		// 模型 ID 即模型目录名；syncModel 保证 name == 目录名 == 策略 modelName()
		return meta.getName();
	}

	private RLTrainingTaskRecord findRecord(String taskId) {
		return repo.findById(taskId)
				.orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId))
				.convertTo();
	}

	private void persistQuietly(RLTrainingTaskRecord record) {
		try {
			// 任务已删除时拒绝落库：异步线程（加载/训练循环）的滞后 save 会把已删记录重新插入（复活）
			if (!repo.existsById(record.getTaskId())) {
				return;
			}
			repo.save(RLTrainingTaskDO.convertFrom(record));
		} catch (Exception e) {
			log.warn("RL 任务状态持久化失败: {}", e.getMessage());
		}
	}

	private void notify(RLTrainingTaskRecord record) {
		try {
			// 任务已删除时不再推送，避免前端列表出现幽灵任务
			if (!repo.existsById(record.getTaskId())) {
				return;
			}
			emitterHolder.send(SseEmitter.event().name(SSE_EVENT).data(toVO(record)));
		} catch (Exception e) {
			log.warn("RL 任务状态推送失败: {}", e.getMessage());
		}
	}

	private RLTrainingTaskVO toVO(RLTrainingTaskRecord r) {
		RLRoundMetrics latest = r.getMetrics() == null || r.getMetrics().isEmpty()
				? null
				: r.getMetrics().get(r.getMetrics().size() - 1);
		return RLTrainingTaskVO.builder()
				.taskId(r.getTaskId())
				.strategyClass(r.getStrategyClass())
				.strategyName(r.getStrategyName())
				.modelId(r.getModelId())
				.modelName(r.getModelName())
				.algo(r.getAlgo())
				.baseVersion(r.getBaseVersion())
				.targetVersion(r.getTargetVersion())
				.contracts(r.getContracts())
				.warmupStartDate(r.getWarmupStartDate())
				.trainStartDate(r.getTrainStartDate())
				.validationStartDate(r.getValidationStartDate())
				.totalRounds(r.getTotalRounds())
				.episodesPerRound(r.getEpisodesPerRound())
				.initBalance(r.getInitBalance())
				.maxHoldingBars(r.getMaxHoldingBars())
				.ruinThreshold(r.getRuinThreshold())
				.maxEpisodeBars(r.getMaxEpisodeBars())
				.status(r.getStatus())
				.currentRound(r.getCurrentRound())
				.dataLoadedContracts(r.getDataLoadedContracts())
				.dataLoadedBars(r.getDataLoadedBars())
				.dataCurrentContract(r.getDataCurrentContract())
				.createdAt(r.getCreatedAt())
				.hyperParams(r.getHyperParams())
				.latestAvgReward(latest == null ? null : latest.getAvgReward())
				.latestLoss(latest == null ? null : (Double.isNaN(latest.getLoss()) ? null : latest.getLoss()))
				.failReason(r.getFailReason())
				.build();
	}

}
