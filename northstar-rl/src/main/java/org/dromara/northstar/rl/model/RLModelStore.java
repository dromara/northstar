package org.dromara.northstar.rl.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.dromara.northstar.strategy.rl.PreprocessType;
import org.dromara.northstar.strategy.rl.RLModelSpec;
import org.dromara.northstar.strategy.rl.RLStatePreprocessor;
import org.dromara.northstar.strategy.rl.RLStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * 模型目录仓库：管理 models 根目录下的模型与版本（文件契约见设计文档 §3.2）。
 *
 * <pre>
 * {modelsRoot}/{modelId}/
 * ├── metadata.json              # 模型级传输载荷（Java 拥有、可重建）
 * └── versions/v{n}/
 *     ├── metadata.json          # 版本级：血缘、超参取值、训练数据统计、评估指标、网络结构（Python 回写）
 *     ├── checkpoint.pt          # 训练态全量（policy/target/optimizer）
 *     ├── model.pt               # TorchScript 推理产物
 *     ├── buffer.npz             # 持久化 replay buffer（Python 跨轮状态）
 *     ├── ingested.json          # 已入库经验分片清单（增量读取依据）
 *     └── rounds/                # 每轮经验数据分片 replay-r{r}-c{m}.csv
 * </pre>
 *
 * 模型级 metadata 不是身份载体而是传输协议：定义权在代码（策略现场 spec + Python 按模型名的
 * 网络注册表），metadata.json 缺失/陈旧时由 {@link #syncModel(RLStrategy)} 现场重建。
 *
 * 已训练版本目录只增不改，规避 Windows 文件锁问题，同时保证模组绑定行为可复盘；
 * 未训练版本（任务未启动）可在任务删除/创建失败时由 deleteVersion 回收，支持非 LIFO 顺序。
 */
@Slf4j
public class RLModelStore {

	private final Path root;
	/** 已注册策略（modelName -> 策略实例）：listModels 遇到无 metadata 的目录时据此现场重建 */
	private final Map<String, RLStrategy> strategies = new ConcurrentHashMap<>();

	public RLModelStore(Path root) {
		this.root = root;
	}

	public RLModelStore(Path root, Collection<RLStrategy> strategies) {
		this(root);
		strategies.forEach(this::registerStrategy);
	}

	public void registerStrategy(RLStrategy strategy) {
		strategies.put(strategy.modelName(), strategy);
	}

	public Path root() {
		return root;
	}

	public Path modelDir(String modelId) {
		return root.resolve(modelId);
	}

	public Path versionDir(String modelId, int version) {
		return modelDir(modelId).resolve("versions").resolve("v" + version);
	}

	public Path roundsDir(String modelId, int version) {
		return versionDir(modelId, version).resolve("rounds");
	}

	public Path modelFile(String modelId, int version) {
		return versionDir(modelId, version).resolve("model.pt");
	}

	public Path checkpointFile(String modelId, int version) {
		return versionDir(modelId, version).resolve("checkpoint.pt");
	}

	/**
	 * 列出全部模型 = 磁盘目录扫描 ∪ 已注册策略声明的模型：
	 * <ul>
	 *   <li>扫描 models 根目录一级子目录；metadata.json 缺失或损坏时，若目录名与已注册策略的
	 *       modelName() 匹配则用其现场 spec 重建，否则跳过并记 warn 日志
	 *       （如 Python 示例环境 cartpole 无对应 Java 策略，按预期被跳过）</li>
	 *   <li>已注册策略声明的模型即使目录尚不存在也收录（现场 bootstrap），
	 *       保证前端"按策略选模型"永远有候选，不必预先手工准备模型目录</li>
	 * </ul>
	 */
	public List<RLModelMetadata> listModels() {
		Map<String, RLModelMetadata> result = new java.util.LinkedHashMap<>();
		if (Files.isDirectory(root)) {
			try (Stream<Path> stream = Files.list(root)) {
				stream.filter(Files::isDirectory)
						.map(dir -> loadOrRebuild(dir.getFileName().toString()))
						.flatMap(Optional::stream)
						.forEach(m -> result.put(m.getName(), m));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		strategies.values().forEach(strategy -> {
			String modelId = strategy.modelName();
			if (!result.containsKey(modelId)) {
				result.put(modelId, syncModel(strategy));
			}
		});
		return List.copyOf(result.values());
	}

	/** 加载模型 metadata；文件缺失/损坏时按目录名匹配已注册策略现场重建，无匹配则跳过 */
	private Optional<RLModelMetadata> loadOrRebuild(String modelId) {
		Path dir = modelDir(modelId);
		try {
			if (Files.isRegularFile(dir.resolve(RLModelMetadata.FILE_NAME))) {
				return Optional.of(RLModelMetadata.load(dir));
			}
		} catch (Exception e) {
			log.warn("RL 模型 {} 的 metadata.json 损坏（{}），尝试按策略现场 spec 重建", modelId, e.getMessage());
		}
		RLStrategy strategy = strategies.get(modelId);
		if (strategy == null) {
			log.warn("RL 模型目录 {} 无 metadata.json 且无同名注册策略，跳过收录", modelId);
			return Optional.empty();
		}
		return Optional.of(syncModel(strategy));
	}

	/** 按目录名加载模型 metadata（仅当文件存在且可解析时返回） */
	public Optional<RLModelMetadata> findModel(String modelId) {
		Path dir = modelDir(modelId);
		if (!Files.isRegularFile(dir.resolve(RLModelMetadata.FILE_NAME))) {
			return Optional.empty();
		}
		try {
			return Optional.of(RLModelMetadata.load(dir));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 以策略现场 spec 为准同步模型级 metadata（bootstrap 或重写陈旧内容）：
	 * <ul>
	 *   <li>目录/metadata.json 不存在：创建目录并写出 metadata.json
	 *       （name/strategy_class/algo/state_dim/action_dim/actions/features/spec_hash 全部来自现场声明，
	 *        latest_version=0，preprocessing 只写 type）</li>
	 *   <li>已存在但 algo 与策略声明不一致：快速失败（checkpoint 结构与超参历史按算法解释，不得跨算法复用，
	 *       变更算法须换新 modelName 建新模型）</li>
	 *   <li>已存在且 spec_hash 一致：原样返回</li>
	 *   <li>已存在但 spec_hash 不一致（陈旧）：重写全部可变字段，保留 latest_version 与 preprocessing.mean/std</li>
	 * </ul>
	 *
	 * @return 同步后的模型 metadata
	 */
	public synchronized RLModelMetadata syncModel(RLStrategy strategy) {
		String modelId = strategy.modelName();
		RLModelSpec spec = strategy.rlSpec();
		validateCustomPreprocessor(modelId, strategy, spec);
		Path dir = modelDir(modelId);
		RLModelMetadata meta = null;
		if (Files.isRegularFile(dir.resolve(RLModelMetadata.FILE_NAME))) {
			try {
				meta = RLModelMetadata.load(dir);
			} catch (Exception e) {
				log.warn("RL 模型 {} 的 metadata.json 损坏（{}），按策略现场 spec 重建", modelId, e.getMessage());
				meta = null;
			}
		}
		if (meta == null) {
			meta = new RLModelMetadata();
			meta.setName(modelId);
			meta.setLatestVersion(0);
			applySpec(meta, strategy, spec);
			try {
				meta.save(dir);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			log.info("RL 模型 {} 无 metadata.json，已按策略现场 spec 引导生成", modelId);
			return meta;
		}
		if (meta.getAlgo() != strategy.rlAlgo()) {
			throw new IllegalStateException(String.format(
					"RL 模型 %s 的算法 %s 与策略声明 %s 不一致：checkpoint 结构与超参历史按算法解释，不得跨算法复用，"
							+ "变更算法请更换 modelName 建新模型",
					modelId, meta.getAlgo(), strategy.rlAlgo()));
		}
		if (spec.specHash().equals(meta.getSpecHash())) {
			return meta;
		}
		// 陈旧重写：latest_version 是版本资产指针、preprocessing.mean/std 是训练侧统计量，均不得丢失
		int latestVersion = meta.getLatestVersion();
		float[] mean = meta.getPreprocessing().getMean();
		float[] std = meta.getPreprocessing().getStd();
		log.warn("RL 模型 {} 的 metadata 与策略现场 spec 不一致（specHash {} != {}），重写可变字段（保留 latest_version 与预处理统计量）",
				modelId, meta.getSpecHash(), spec.specHash());
		applySpec(meta, strategy, spec);
		meta.setLatestVersion(latestVersion);
		meta.getPreprocessing().setType(spec.preprocessing().code());
		meta.getPreprocessing().setMean(mean);
		meta.getPreprocessing().setStd(std);
		try {
			meta.save(dir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return meta;
	}

	/**
	 * CUSTOM 预处理的创建期校验：声明了 CUSTOM 就必须提供可用的 {@link RLStatePreprocessor}，
	 * 在创建训练任务时（syncModel 必经）暴露，而不是等到训练/推理期才失败。
	 */
	private static void validateCustomPreprocessor(String modelId, RLStrategy strategy, RLModelSpec spec) {
		if (spec.preprocessing() != PreprocessType.CUSTOM) {
			return;
		}
		RLStatePreprocessor preprocessor;
		try {
			preprocessor = strategy.rlPreprocessor();
		} catch (UnsupportedOperationException e) {
			throw new IllegalStateException(String.format(
					"RL 模型 %s 的 preprocessing 声明为 CUSTOM，但策略 %s 未实现 rlPreprocessor()",
					modelId, strategy.getClass().getName()), e);
		}
		if (preprocessor == null) {
			throw new IllegalStateException(String.format(
					"RL 模型 %s 的 preprocessing 声明为 CUSTOM，但策略 %s 的 rlPreprocessor() 返回 null",
					modelId, strategy.getClass().getName()));
		}
	}

	/** 以策略现场声明重写 metadata 的可变字段（name/strategy_class/algo/state_dim/action_dim/actions/features/spec_hash 与预处理类型） */
	private static void applySpec(RLModelMetadata meta, RLStrategy strategy, RLModelSpec spec) {
		meta.setName(strategy.modelName());
		meta.setStrategyClass(strategy.getClass().getName());
		meta.setAlgo(strategy.rlAlgo());
		meta.setStateDim(spec.stateDim());
		meta.setActionDim(spec.actionDim());
		meta.setActions(spec.actions());
		meta.setFeatures(spec.features().stream()
				.map(f -> new RLModelMetadata.FeatureInfo(f.name(), f.normalizer().name()))
				.toList());
		meta.getPreprocessing().setType(spec.preprocessing().code());
		meta.setSpecHash(spec.specHash());
	}

	/** 列出某模型的全部版本（按版本号升序） */
	public List<RLVersionMetadata> listVersions(String modelId) {
		Path versionsDir = modelDir(modelId).resolve("versions");
		if (!Files.isDirectory(versionsDir)) {
			return List.of();
		}
		try (Stream<Path> stream = Files.list(versionsDir)) {
			return stream
					.filter(dir -> Files.isRegularFile(dir.resolve(RLVersionMetadata.FILE_NAME)))
					.map(dir -> {
						try {
							return RLVersionMetadata.load(dir);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					})
					.sorted(Comparator.comparingInt(RLVersionMetadata::getVersion))
					.toList();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Optional<RLVersionMetadata> findVersion(String modelId, int version) {
		Path dir = versionDir(modelId, version);
		if (!Files.isDirectory(dir)) {
			return Optional.empty();
		}
		try {
			return Optional.of(RLVersionMetadata.load(dir));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * fork 新版本：版本号 = latestVersion + 1。
	 *
	 * 双重校验：父版本 specHash 必须与策略现场 spec 一致；inheritWeights 时父版本必须存在已训练的 checkpoint。
	 * 预处理统计量默认沿用父版本（保证输入分布一致，权重继承才有意义）。
	 * 模型级 metadata 缺失/陈旧时先由现场 spec 引导重建，不会因 metadata 问题失败。
	 *
	 * @param strategy			模型绑定策略（modelName 即模型目录名）
	 * @param baseVersion		基准版本；0 表示从零训练
	 * @param inheritWeights	false = 不复制 checkpoint 从零训练，但继承特征定义与数据配置
	 * @param hyperParams		新版本超参数取值
	 * @param training			训练信息（时段/合约/种子等）
	 */
	public synchronized RLVersionMetadata forkVersion(RLStrategy strategy, int baseVersion, boolean inheritWeights,
			Map<String, Double> hyperParams, RLVersionMetadata.TrainingInfo training) {
		String modelId = strategy.modelName();
		RLModelMetadata model = syncModel(strategy);
		if (inheritWeights && baseVersion <= 0) {
			throw new IllegalArgumentException("继承权重训练时 baseVersion 必须为已存在的历史版本（> 0）");
		}

		String liveHash = strategy.rlSpec().specHash();
		RLVersionMetadata parent = null;
		if (baseVersion > 0) {
			parent = findVersion(modelId, baseVersion)
					.orElseThrow(() -> new IllegalArgumentException("基准版本不存在: " + modelId + " v" + baseVersion));
			if (!liveHash.equals(parent.getSpecHash())) {
				throw new IllegalStateException(String.format(
						"父版本 specHash 与策略现场规格不一致：%s != %s，拒绝 fork", parent.getSpecHash(), liveHash));
			}
			if (inheritWeights && !Files.isRegularFile(checkpointFile(modelId, baseVersion))) {
				throw new IllegalStateException(String.format(
						"父版本 v%d 缺少已训练的 checkpoint，无法继承权重（可选择 inheritWeights=false 从零训练）", baseVersion));
			}
		}

		int newVersion = model.getLatestVersion() + 1;
		Path versionDir = versionDir(modelId, newVersion);
		try {
			Files.createDirectories(roundsDir(modelId, newVersion));
			if (parent != null && inheritWeights) {
				Files.copy(checkpointFile(modelId, baseVersion), checkpointFile(modelId, newVersion));
			}

			RLVersionMetadata ver = new RLVersionMetadata();
			ver.setVersion(newVersion);
			ver.setParentVersion(baseVersion);
			ver.setWeightInherited(inheritWeights && parent != null);
			ver.setSpecHash(liveHash);
			// 预处理统计量冻结：fork 默认沿用父版本；从零训练时留空待首次训练计算
			if (parent != null) {
				ver.setPreprocessing(parent.getPreprocessing());
			} else {
				ver.setPreprocessing(model.getPreprocessing());
			}
			training.setHyperParams(hyperParams);
			ver.setTraining(training);
			ver.save(versionDir);

			model.setLatestVersion(newVersion);
			model.save(modelDir(modelId));
			return ver;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 删除版本（创建失败/任务删除回收用）：删除版本目录；仅当删除的是最新版本时回退
	 * latest_version 至剩余最大版本号（无剩余则为 0）。
	 *
	 * 同一模型可同时存在多个训练任务 fork 的版本，删除顺序不保证后进先出，
	 * 因此非最新版本同样允许回收：版本号单调递增由 forkVersion（latestVersion + 1）保证，
	 * 子版本在 fork 时已复制父版本 checkpoint，删除父版本目录不影响子版本。
	 *
	 * 模型级 metadata 缺失时不阻塞版本回收：照常删除版本目录，仅跳过 latest_version 回退。
	 */
	public synchronized void deleteVersion(String modelId, int version) {
		Optional<RLModelMetadata> modelOpt = findModel(modelId);
		Path versionDir = versionDir(modelId, version);
		try {
			if (Files.isDirectory(versionDir)) {
				try (Stream<Path> stream = Files.walk(versionDir)) {
					stream.sorted(Comparator.reverseOrder())
							.forEach(p -> {
								try {
									Files.delete(p);
								} catch (IOException e) {
									throw new UncheckedIOException(e);
								}
							});
				}
			}
			if (modelOpt.isEmpty()) {
				log.warn("RL 模型 {} 无 metadata.json，删除版本 v{} 后跳过 latest_version 回退", modelId, version);
				return;
			}
			RLModelMetadata model = modelOpt.get();
			if (model.getLatestVersion() == version) {
				model.setLatestVersion(maxRemainingVersion(modelId));
				model.save(modelDir(modelId));
			}
		} catch (UncheckedIOException e) {
			throw new UncheckedIOException("删除版本目录失败: " + versionDir, e.getCause());
		} catch (IOException e) {
			throw new UncheckedIOException("删除版本目录失败: " + versionDir, e);
		}
	}

	/** 剩余版本号的最大值（版本目录可能因非 LIFO 回收出现空洞，不能简单取 latestVersion - 1） */
	private int maxRemainingVersion(String modelId) {
		Path versionsDir = modelDir(modelId).resolve("versions");
		if (!Files.isDirectory(versionsDir)) {
			return 0;
		}
		try (Stream<Path> stream = Files.list(versionsDir)) {
			return stream.filter(Files::isDirectory)
					.map(p -> p.getFileName().toString())
					.filter(name -> name.matches("v\\d+"))
					.mapToInt(name -> Integer.parseInt(name.substring(1)))
					.max()
					.orElse(0);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
