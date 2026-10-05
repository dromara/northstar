package org.dromara.northstar.rl.model;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.dromara.northstar.strategy.rl.PreprocessType;
import org.dromara.northstar.strategy.rl.RLFeatureSpec;
import org.dromara.northstar.strategy.rl.RLModelSpec;

import ai.djl.util.JsonUtils;

/**
 * 模型级 metadata（传输协议）：models/{modelId}/metadata.json 的 Java 映射。
 *
 * 定义权在代码（策略现场 spec + Python 按模型名的网络注册表），本文件仅是 Java 写出、
 * Python 读入、可随时重建的传输载荷：缺失时由 Java 用策略现场 spec 引导生成（bootstrap），
 * 陈旧时重写可变字段（保留 latest_version 与 preprocessing.mean/std）。
 *
 * 该文件是 Java 与 Python 的共同契约，字段名一律使用 snake_case。
 * 反序列化容忍旧文件中遗留的 network 等未知字段（Gson 默认忽略），保证旧文件可加载。
 */
public class RLModelMetadata {

	public static final String FILE_NAME = "metadata.json";

	@SerializedName("name")
	private String name;
	/** 绑定的策略类全限定名（示例环境如 CartPole 无绑定策略，可为空） */
	@SerializedName("strategy_class")
	private String strategyClass;
	/** 训练算法（DQN；PPO 预留给 v2） */
	@SerializedName("algo")
	private String algo = "DQN";
	@SerializedName("state_dim")
	private int stateDim;
	@SerializedName("action_dim")
	private int actionDim;
	/** 动作名表：actions[i] = 动作下标 i 对应的名称 */
	@SerializedName("actions")
	private List<String> actions;
	/** 状态特征清单（顺序敏感，顺序即状态向量下标）；specHash 校验失败时据此做结构化 diff */
	@SerializedName("features")
	private List<FeatureInfo> features = List.of();
	@SerializedName("preprocessing")
	private Preprocessing preprocessing = new Preprocessing();
	@SerializedName("spec_hash")
	private String specHash;
	@SerializedName("latest_version")
	private int latestVersion;

	public static class FeatureInfo {
		@SerializedName("name")
		private String name;
		/** 归一化类型（NormalizerType 枚举 name 字符串） */
		@SerializedName("normalizer")
		private String normalizer;

		public FeatureInfo() {
		}

		public FeatureInfo(String name, String normalizer) {
			this.name = name;
			this.normalizer = normalizer;
		}

		public String getName() {
			return name;
		}

		public String getNormalizer() {
			return normalizer;
		}
	}

	public static class Preprocessing {
		@SerializedName("type")
		private String type = "identity";
		@SerializedName("mean")
		private float[] mean = new float[0];
		@SerializedName("std")
		private float[] std = new float[0];

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public float[] getMean() {
			return mean;
		}

		public void setMean(float[] mean) {
			this.mean = mean;
		}

		public float[] getStd() {
			return std;
		}

		public void setStd(float[] std) {
			this.std = std;
		}
	}

	public static RLModelMetadata load(Path modelDir) throws IOException {
		try (Reader reader = Files.newBufferedReader(modelDir.resolve(FILE_NAME))) {
			return JsonUtils.GSON.fromJson(reader, RLModelMetadata.class);
		}
	}

	public void save(Path modelDir) throws IOException {
		Files.createDirectories(modelDir);
		Path target = modelDir.resolve(FILE_NAME);
		Path tmp = target.resolveSibling(FILE_NAME + ".tmp");
		// 先写临时文件再原子替换，规避 Windows 文件锁与崩溃半截写
		try (Writer writer = Files.newBufferedWriter(tmp)) {
			JsonUtils.GSON.toJson(this, writer);
		}
		try {
			Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStrategyClass() {
		return strategyClass;
	}

	public void setStrategyClass(String strategyClass) {
		this.strategyClass = strategyClass;
	}

	public String getAlgo() {
		return algo;
	}

	public void setAlgo(String algo) {
		this.algo = algo;
	}

	public int getStateDim() {
		return stateDim;
	}

	public void setStateDim(int stateDim) {
		this.stateDim = stateDim;
	}

	public int getActionDim() {
		return actionDim;
	}

	public void setActionDim(int actionDim) {
		this.actionDim = actionDim;
	}

	public List<String> getActions() {
		return actions;
	}

	public void setActions(List<String> actions) {
		this.actions = actions;
	}

	/** 状态特征清单；兼容无 features 字段的旧 metadata（按空列表处理） */
	public List<FeatureInfo> getFeatures() {
		return features == null ? List.of() : features;
	}

	public void setFeatures(List<FeatureInfo> features) {
		this.features = features;
	}

	/**
	 * 与策略当前的模型规格做结构化比对（specHash 校验失败时输出差异明细）。
	 *
	 * 模型级 metadata 只是可重建的传输载荷，本方法以其中持久化的 features/actions/preprocessing
	 * 为基准与策略现场 spec 逐字段比对（动作枚举类无法从 actions 名表反建，故不重建 RLModelSpec），
	 * 输出格式与 {@link RLModelSpec#diff} 一致。
	 *
	 * @return 差异描述列表；为空表示两者等价
	 */
	public List<String> diffSpec(RLModelSpec current) {
		List<String> diffs = new ArrayList<>();
		List<FeatureInfo> persisted = getFeatures();
		List<RLFeatureSpec> currentFeatures = current.features();
		int minLen = Math.min(persisted.size(), currentFeatures.size());
		for (int i = 0; i < minLen; i++) {
			FeatureInfo a = persisted.get(i);
			RLFeatureSpec b = currentFeatures.get(i);
			if (!a.getName().equals(b.name()) || !a.getNormalizer().equals(b.normalizer().name())) {
				diffs.add(String.format("特征[%d]变更: %s(%s) -> %s(%s)", i, a.getName(), a.getNormalizer(), b.name(), b.normalizer()));
			}
		}
		for (int i = minLen; i < persisted.size(); i++) {
			diffs.add(String.format("特征[%d]被移除: %s", i, persisted.get(i).getName()));
		}
		for (int i = minLen; i < currentFeatures.size(); i++) {
			diffs.add(String.format("特征[%d]新增: %s", i, currentFeatures.get(i).name()));
		}
		if (!getActions().equals(current.actions())) {
			diffs.add(String.format("动作表变更: %s -> %s", getActions(), current.actions()));
		}
		PreprocessType persistedType = PreprocessType.of(getPreprocessing().getType());
		if (persistedType != current.preprocessing()) {
			diffs.add(String.format("预处理变更: %s -> %s", persistedType, current.preprocessing()));
		}
		return diffs;
	}

	public Preprocessing getPreprocessing() {
		return preprocessing;
	}

	public void setPreprocessing(Preprocessing preprocessing) {
		this.preprocessing = preprocessing;
	}

	public String getSpecHash() {
		return specHash;
	}

	public void setSpecHash(String specHash) {
		this.specHash = specHash;
	}

	public int getLatestVersion() {
		return latestVersion;
	}

	public void setLatestVersion(int latestVersion) {
		this.latestVersion = latestVersion;
	}
}
