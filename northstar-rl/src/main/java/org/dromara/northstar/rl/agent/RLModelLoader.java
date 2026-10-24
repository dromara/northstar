package org.dromara.northstar.rl.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.dromara.northstar.rl.model.RLModelMetadata;
import org.dromara.northstar.rl.model.RLModelStore;
import org.dromara.northstar.rl.model.RLVersionMetadata;
import org.dromara.northstar.strategy.rl.PreprocessType;
import org.dromara.northstar.strategy.rl.RLStatePreprocessor;
import org.dromara.northstar.strategy.rl.RLStrategy;

/**
 * 实盘推理模型的统一加载入口：策略侧只需提供模型名（{@link RLStrategy#modelName()}）与版本号，
 * 版本目录按目录契约（{@code {modelsRoot}/{modelName}/versions/v{n}}）由框架自动解析，
 * 并完成模型级/版本级 specHash 校验（策略演进防护）与权重加载。
 *
 * 模型根目录优先取 Spring 装配发布值（{@link #publishModelsRoot}，含 application.yml 配置），
 * 未发布时按 {@link RLModelStore#resolveRoot} 的规则现场解析（环境变量 NS_RL_MODELS_ROOT → 候选路径）。
 */
public final class RLModelLoader {

	private static volatile Path publishedRoot;

	private RLModelLoader() {
	}

	/** Spring 装配 RLModelStore 时发布框架实际使用的模型根目录，供策略侧实盘加载复用 */
	public static void publishModelsRoot(Path root) {
		publishedRoot = root;
	}

	/** 当前生效的模型根目录：优先装配发布值，否则现场解析 */
	public static Path modelsRoot() {
		Path root = publishedRoot;
		return root != null ? root : RLModelStore.resolveRoot(null);
	}

	/**
	 * 加载实盘推理 Agent（线程安全的惰性加载由调用方负责，本方法每次调用都会重新读盘加载）。
	 *
	 * @param strategy	绑定策略（提供 modelName/rlSpec/rlPreprocessor 现场声明）
	 * @param version	版本号；&le; 0 表示取模型最新版本（latest_version）
	 * @param <A>		动作枚举类型
	 * @return			可推理的 Agent（用毕需关闭）
	 */
	public static <A extends Enum<A>> DqnAgent<A> loadLiveAgent(RLStrategy strategy, Class<A> actionType, int version) {
		String modelId = strategy.modelName();
		RLModelStore store = new RLModelStore(modelsRoot());
		int resolved = version;
		try {
			if (resolved <= 0) {
				resolved = store.findModel(modelId)
						.map(RLModelMetadata::getLatestVersion)
						.orElseThrow(() -> new IllegalStateException(String.format(
								"RL 模型 %s 不存在（模型根目录: %s）", modelId, modelsRoot().toAbsolutePath())));
				if (resolved <= 0) {
					throw new IllegalStateException(String.format(
							"RL 模型 %s 尚无已创建的版本（latest_version=0），请先在训练任务中 fork 版本", modelId));
				}
			}
			Path versionDir = store.versionDir(modelId, resolved);
			if (!Files.isDirectory(versionDir)) {
				throw new IllegalStateException("RL 模型版本目录不存在: " + versionDir.toAbsolutePath());
			}
			RLModelMetadata modelMeta = RLModelMetadata.load(store.modelDir(modelId));
			RLVersionMetadata verMeta = RLVersionMetadata.load(versionDir);
			validateSpec(strategy, modelMeta, verMeta);
			RLStatePreprocessor preprocessor = strategy.rlSpec().preprocessing() == PreprocessType.CUSTOM
					? strategy.rlPreprocessor()
					: null;
			return DqnAgent.fromVersionDir(versionDir, actionType, preprocessor);
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(String.format("RL 推理模型加载失败: %s v%d", modelId, resolved), e);
		}
	}

	/**
	 * 策略演进防护：模型级与版本级 metadata 的 specHash 必须与策略现场 spec 一致，
	 * 不一致时输出结构化 diff（旧 metadata 无 features 字段时 diff 为空，仅报 hash 不一致）。
	 */
	private static void validateSpec(RLStrategy strategy, RLModelMetadata modelMeta, RLVersionMetadata verMeta) {
		String liveHash = strategy.rlSpec().specHash();
		if (liveHash.equals(modelMeta.getSpecHash()) && liveHash.equals(verMeta.getSpecHash())) {
			return;
		}
		List<String> diffs = modelMeta.diffSpec(strategy.rlSpec());
		throw new IllegalStateException("模型规格与当前策略不一致（specHash 校验失败），请更换匹配的模型版本"
				+ (diffs.isEmpty() ? "" : "：\n" + String.join("\n", diffs)));
	}
}
