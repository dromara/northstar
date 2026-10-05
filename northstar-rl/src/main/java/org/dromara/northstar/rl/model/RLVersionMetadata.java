package org.dromara.northstar.rl.model;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import ai.djl.util.JsonUtils;

/**
 * 版本级 metadata（出生证明）：models/{modelId}/versions/v{n}/metadata.json 的 Java 映射。
 *
 * 任何一个线上模组绑定的模型版本，都能反推出：从哪个版本继承权重、用了什么特征定义、
 * 在哪个数据区间训练、凭什么被判定达标。
 */
public class RLVersionMetadata {

	public static final String FILE_NAME = "metadata.json";

	@SerializedName("version")
	private int version;
	/** 父版本号；0 表示从零训练 */
	@SerializedName("parent_version")
	private int parentVersion;
	/** 是否继承了父版本权重（false = 继承特征定义与数据配置但从零训练，语义诚实） */
	@SerializedName("weight_inherited")
	private boolean weightInherited;
	@SerializedName("spec_hash")
	private String specHash;
	/**
	 * 网络结构信息（如 {"type":"mlp","hidden":[128,128],"activation":"relu"}），
	 * 由 Python 每轮训练后写入；Java 当作不透明数据读取并透传到 VO，不解析其语义
	 */
	@SerializedName("network")
	private Map<String, Object> network;
	/** 各层参数 shape 哈希 + optimizer 类型 + 累计步数，由 Python 训练侧写出 */
	@SerializedName("checkpoint_signature")
	private String checkpointSignature;
	/** 预处理统计量：v1 首次训练时计算并冻结；fork 默认沿用父版本统计量 */
	@SerializedName("preprocessing")
	private RLModelMetadata.Preprocessing preprocessing = new RLModelMetadata.Preprocessing();
	@SerializedName("training")
	private TrainingInfo training = new TrainingInfo();
	/** 达标判定结果（验证时段回测指标），由评估器写入 */
	@SerializedName("accepted_by")
	private AcceptedBy acceptedBy;

	public static class TrainingInfo {
		@SerializedName("rounds")
		private int rounds;
		@SerializedName("warmup_start_date")
		private String warmupStartDate;
		@SerializedName("train_start_date")
		private String trainStartDate;
		@SerializedName("validation_start_date")
		private String validationStartDate;
		@SerializedName("contracts")
		private List<String> contracts;
		/** 该版本的超参数取值（字段清单由模型的 Hyperparameter 类定义） */
		@SerializedName("hyper_params")
		private Map<String, Double> hyperParams;
		@SerializedName("seed")
		private long seed;
		/** 决策点触发条件摘要（触发条件属策略语义，不进 specHash，但需记录保证可复盘） */
		@SerializedName("trigger_summary")
		private String triggerSummary;

		public int getRounds() {
			return rounds;
		}

		public void setRounds(int rounds) {
			this.rounds = rounds;
		}

		public String getWarmupStartDate() {
			return warmupStartDate;
		}

		public void setWarmupStartDate(String warmupStartDate) {
			this.warmupStartDate = warmupStartDate;
		}

		public String getTrainStartDate() {
			return trainStartDate;
		}

		public void setTrainStartDate(String trainStartDate) {
			this.trainStartDate = trainStartDate;
		}

		public String getValidationStartDate() {
			return validationStartDate;
		}

		public void setValidationStartDate(String validationStartDate) {
			this.validationStartDate = validationStartDate;
		}

		public List<String> getContracts() {
			return contracts;
		}

		public void setContracts(List<String> contracts) {
			this.contracts = contracts;
		}

		public Map<String, Double> getHyperParams() {
			return hyperParams;
		}

		public void setHyperParams(Map<String, Double> hyperParams) {
			this.hyperParams = hyperParams;
		}

		public long getSeed() {
			return seed;
		}

		public void setSeed(long seed) {
			this.seed = seed;
		}

		public String getTriggerSummary() {
			return triggerSummary;
		}

		public void setTriggerSummary(String triggerSummary) {
			this.triggerSummary = triggerSummary;
		}
	}

	public static class AcceptedBy {
		@SerializedName("win_rate")
		private double winRate;
		@SerializedName("profit_factor")
		private double profitFactor;
		@SerializedName("trades")
		private int trades;

		public AcceptedBy() {
		}

		public AcceptedBy(double winRate, double profitFactor, int trades) {
			this.winRate = winRate;
			this.profitFactor = profitFactor;
			this.trades = trades;
		}

		public double getWinRate() {
			return winRate;
		}

		public double getProfitFactor() {
			return profitFactor;
		}

		public int getTrades() {
			return trades;
		}
	}

	public static RLVersionMetadata load(Path versionDir) throws IOException {
		try (Reader reader = Files.newBufferedReader(versionDir.resolve(FILE_NAME))) {
			return JsonUtils.GSON.fromJson(reader, RLVersionMetadata.class);
		}
	}

	public void save(Path versionDir) throws IOException {
		Files.createDirectories(versionDir);
		Path target = versionDir.resolve(FILE_NAME);
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

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public int getParentVersion() {
		return parentVersion;
	}

	public void setParentVersion(int parentVersion) {
		this.parentVersion = parentVersion;
	}

	public boolean isWeightInherited() {
		return weightInherited;
	}

	public void setWeightInherited(boolean weightInherited) {
		this.weightInherited = weightInherited;
	}

	public String getSpecHash() {
		return specHash;
	}

	public void setSpecHash(String specHash) {
		this.specHash = specHash;
	}

	public Map<String, Object> getNetwork() {
		return network;
	}

	public void setNetwork(Map<String, Object> network) {
		this.network = network;
	}

	public String getCheckpointSignature() {
		return checkpointSignature;
	}

	public void setCheckpointSignature(String checkpointSignature) {
		this.checkpointSignature = checkpointSignature;
	}

	public RLModelMetadata.Preprocessing getPreprocessing() {
		return preprocessing;
	}

	public void setPreprocessing(RLModelMetadata.Preprocessing preprocessing) {
		this.preprocessing = preprocessing;
	}

	public TrainingInfo getTraining() {
		return training;
	}

	public void setTraining(TrainingInfo training) {
		this.training = training;
	}

	public AcceptedBy getAcceptedBy() {
		return acceptedBy;
	}

	public void setAcceptedBy(AcceptedBy acceptedBy) {
		this.acceptedBy = acceptedBy;
	}
}
