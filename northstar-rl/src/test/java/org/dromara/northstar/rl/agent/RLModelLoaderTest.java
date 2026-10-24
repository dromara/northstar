package org.dromara.northstar.rl.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.rl.model.RLModelStore;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.IModuleStrategyContext;
import org.dromara.northstar.strategy.rl.NormalizerType;
import org.dromara.northstar.strategy.rl.PreprocessType;
import org.dromara.northstar.strategy.rl.RLFeatureSpec;
import org.dromara.northstar.strategy.rl.RLModelSpec;
import org.dromara.northstar.strategy.rl.RLRewardContext;
import org.dromara.northstar.strategy.rl.RLStrategy;
import org.dromara.northstar.strategy.rl.TradeAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * RLModelLoader 的模型名 + 版本号自动寻址验证。
 * 全部用例在校验阶段快速失败，不会触达 PyTorch 原生库加载。
 */
class RLModelLoaderTest {

	@TempDir
	Path root;

	private final RLModelSpec spec = new RLModelSpec(List.of(
			new RLFeatureSpec("ret1", NormalizerType.RATIO),
			new RLFeatureSpec("maDev", NormalizerType.RATIO)),
			TradeAction.class, PreprocessType.IDENTITY);

	private final RLStrategy strategy = new FakeStrategy("demo", spec);

	@BeforeEach
	void publishRoot() {
		RLModelLoader.publishModelsRoot(root);
	}

	@AfterEach
	void resetRoot() {
		RLModelLoader.publishModelsRoot(null);
	}

	/** 写入模型级 metadata（spec_hash 可指定以模拟策略演进前后的各种组合） */
	private void writeModelMeta(int latestVersion, String specHash, boolean withFeatures) throws Exception {
		Path dir = root.resolve("demo");
		Files.createDirectories(dir);
		String features = withFeatures
				? "\"features\":[{\"name\":\"ret1\",\"normalizer\":\"RATIO\"},{\"name\":\"oldFeature\",\"normalizer\":\"RATIO\"}],"
				: "";
		Files.writeString(dir.resolve("metadata.json"), """
				{"name":"demo","state_dim":2,"action_dim":3,"actions":["BUY","SELL","HOLD"],%s
				"spec_hash":"%s","latest_version":%d}
				""".formatted(features, specHash, latestVersion));
	}

	private void writeVersionMeta(int version, String specHash) throws Exception {
		Path dir = root.resolve("demo/versions/v" + version);
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("metadata.json"), """
				{"version":%d,"spec_hash":"%s","preprocessing":{"type":"identity"}}
				""".formatted(version, specHash));
	}

	@Test
	void resolvesVersionDirByModelNameAndVersion() throws Exception {
		// 版本目录完全由 modelName() + 版本号按目录契约寻址，命中即进入 specHash 校验
		writeModelMeta(1, "stale-hash", false);
		writeVersionMeta(1, "stale-hash");
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> RLModelLoader.loadLiveAgent(strategy, TradeAction.class, 1));
		assertTrue(e.getMessage().contains("specHash 校验失败"), e.getMessage());
	}

	@Test
	void mismatchOnVersionLevelAlsoRejected() throws Exception {
		writeModelMeta(1, spec.specHash(), false);
		writeVersionMeta(1, "stale-hash");
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> RLModelLoader.loadLiveAgent(strategy, TradeAction.class, 1));
		assertTrue(e.getMessage().contains("specHash 校验失败"), e.getMessage());
	}

	@Test
	void nonBlankDiffWhenMetadataHasFeatures() throws Exception {
		writeModelMeta(1, "stale-hash", true);
		writeVersionMeta(1, "stale-hash");
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> RLModelLoader.loadLiveAgent(strategy, TradeAction.class, 1));
		assertTrue(e.getMessage().contains("oldFeature"), e.getMessage());
	}

	@Test
	void nonPositiveVersionResolvesToLatest() throws Exception {
		// latest_version=2 且 v2 目录未创建：报错信息应体现解析后的版本号，证明自动寻址到 v2
		writeModelMeta(2, "stale-hash", false);
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> RLModelLoader.loadLiveAgent(strategy, TradeAction.class, 0));
		assertTrue(e.getMessage().contains("v2"), e.getMessage());
	}

	@Test
	void failsWhenVersionDirMissing() throws Exception {
		writeModelMeta(1, "stale-hash", false);
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> RLModelLoader.loadLiveAgent(strategy, TradeAction.class, 3));
		assertTrue(e.getMessage().contains("版本目录不存在"), e.getMessage());
	}

	@Test
	void failsWhenModelMissing() {
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> RLModelLoader.loadLiveAgent(strategy, TradeAction.class, 1));
		assertTrue(e.getMessage().contains("版本目录不存在"), e.getMessage());
	}

	@Test
	void failsWhenModelHasNoVersionYet() throws Exception {
		writeModelMeta(0, "stale-hash", false);
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> RLModelLoader.loadLiveAgent(strategy, TradeAction.class, 0));
		assertTrue(e.getMessage().contains("尚无已创建的版本"), e.getMessage());
	}

	@Test
	void resolveRootPrefersExistingConfiguredDir() {
		assertEquals(root, RLModelStore.resolveRoot(root.toString()));
	}

	/** 测试用策略桩：与 RLModelStoreTest.FakeStrategy 同款最小实现 */
	static class FakeStrategy extends AbstractStrategy implements RLStrategy {
		private final String modelName;
		private final RLModelSpec spec;

		FakeStrategy(String modelName, RLModelSpec spec) {
			this.modelName = modelName;
			this.spec = spec;
		}

		@Override
		public String name() {
			return modelName;
		}

		@Override
		public String modelName() {
			return modelName;
		}

		@Override
		public RLModelSpec rlSpec() {
			return spec;
		}

		@Override
		public DynamicParams getDynamicParams() {
			return null;
		}

		@Override
		public void initWithParams(DynamicParams params) {
		}

		@Override
		public void onTick(Tick tick) {
		}

		@Override
		public void onMergedBar(Bar bar) {
		}

		@Override
		public float[] observe(IModuleStrategyContext ctx, Contract contract) {
			return new float[spec.stateDim()];
		}

		@Override
		public void onAction(TradeAction action, IModuleStrategyContext ctx, Contract contract) {
		}

		@Override
		public boolean shouldObserve(IModuleStrategyContext ctx, Contract contract) {
			return false;
		}

		@Override
		public double reward(RLRewardContext ctx) {
			return 0;
		}
	}
}
