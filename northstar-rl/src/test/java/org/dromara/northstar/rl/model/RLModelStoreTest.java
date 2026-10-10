package org.dromara.northstar.rl.model;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.IModuleStrategyContext;
import org.dromara.northstar.strategy.rl.NormalizerType;
import org.dromara.northstar.strategy.rl.PreprocessType;
import org.dromara.northstar.strategy.rl.RLAlgo;
import org.dromara.northstar.strategy.rl.RLFeatureSpec;
import org.dromara.northstar.strategy.rl.RLModelSpec;
import org.dromara.northstar.strategy.rl.RLRewardContext;
import org.dromara.northstar.strategy.rl.RLStatePreprocessor;
import org.dromara.northstar.strategy.rl.RLStrategy;
import org.dromara.northstar.strategy.rl.TradeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RLModelStoreTest {

    @TempDir
    Path root;

    private RLModelStore store;

    private final RLModelSpec spec = new RLModelSpec(List.of(
            new RLFeatureSpec("ret1", NormalizerType.RATIO),
            new RLFeatureSpec("maDev", NormalizerType.RATIO)),
            TradeAction.class, PreprocessType.IDENTITY);

    /** 测试用策略桩：modelName/spec 可参数化，用于模拟策略演进（换 spec 同名重建） */
    static class FakeStrategy extends AbstractStrategy implements RLStrategy {
        private final String modelName;
        private final RLModelSpec spec;
        private RLAlgo algo = RLAlgo.DQN;
        private RLStatePreprocessor preprocessor;

        FakeStrategy(String modelName, RLModelSpec spec) {
            this.modelName = modelName;
            this.spec = spec;
        }

        FakeStrategy withAlgo(RLAlgo algo) {
            this.algo = algo;
            return this;
        }

        FakeStrategy withPreprocessor(RLStatePreprocessor preprocessor) {
            this.preprocessor = preprocessor;
            return this;
        }

        @Override
        public RLAlgo rlAlgo() {
            return algo;
        }

        @Override
        public RLStatePreprocessor rlPreprocessor() {
            return preprocessor;
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

    @BeforeEach
    void setUp() {
        store = new RLModelStore(root);
    }

    private RLVersionMetadata.TrainingInfo training() {
        RLVersionMetadata.TrainingInfo t = new RLVersionMetadata.TrainingInfo();
        t.setRounds(30);
        t.setWarmupStartDate("2022-01-01");
        t.setTrainStartDate("2023-01-01");
        t.setValidationStartDate("2024-07-01");
        t.setContracts(List.of("rb0000@SHFE@FUTURES"));
        t.setSeed(42);
        return t;
    }

    /* ---------- syncModel：bootstrap / 幂等 / 陈旧重写 ---------- */

    @Test
    void syncModelBootstrapsMissingMetadataFromLiveSpec() throws Exception {
        FakeStrategy strategy = new FakeStrategy("rb-trend-dqn", spec);
        RLModelMetadata meta = store.syncModel(strategy);

        assertEquals("rb-trend-dqn", meta.getName());
        assertEquals(FakeStrategy.class.getName(), meta.getStrategyClass());
        assertEquals(RLAlgo.DQN, meta.getAlgo());
        assertEquals(2, meta.getStateDim());
        assertEquals(3, meta.getActionDim());
        assertEquals(List.of("HOLD", "BUY", "SELL"), meta.getActions());
        assertEquals("identity", meta.getPreprocessing().getType());
        assertEquals(0, meta.getPreprocessing().getMean().length, "bootstrap 只写 type，不写统计量");
        assertEquals(0, meta.getPreprocessing().getStd().length);
        assertEquals(2, meta.getFeatures().size());
        assertEquals("ret1", meta.getFeatures().get(0).getName());
        assertEquals("RATIO", meta.getFeatures().get(0).getNormalizer());
        assertEquals("maDev", meta.getFeatures().get(1).getName());
        assertEquals(spec.specHash(), meta.getSpecHash());
        assertEquals(0, meta.getLatestVersion());
        assertTrue(Files.isRegularFile(store.modelDir("rb-trend-dqn").resolve(RLModelMetadata.FILE_NAME)));
    }

    @Test
    void syncModelIsIdempotentWhenSpecUnchanged() {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);
        store.forkVersion(strategy, 0, false, Map.of(), training());	// latest_version 推进到 1

        RLModelMetadata meta = store.syncModel(strategy);
        assertEquals(spec.specHash(), meta.getSpecHash());
        assertEquals(1, meta.getLatestVersion(), "spec 未变化时不得触碰版本资产指针");
    }

    @Test
    void syncModelRewritesStaleMetadataKeepingVersionAndPreprocessingStats() throws Exception {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);
        store.forkVersion(strategy, 0, false, Map.of(), training());
        // 模拟 Python 训练侧写回的预处理统计量
        RLModelMetadata trained = store.findModel("m").orElseThrow();
        trained.getPreprocessing().setType("standardize");
        trained.getPreprocessing().setMean(new float[] {1, 2});
        trained.getPreprocessing().setStd(new float[] {3, 4});
        trained.save(store.modelDir("m"));

        // 策略演进：同名模型、规格变化（Z_SCORE + 新增特征 + 预处理变更）
        RLModelSpec evolved = new RLModelSpec(List.of(
                new RLFeatureSpec("ret1", NormalizerType.Z_SCORE),
                new RLFeatureSpec("maDev", NormalizerType.RATIO),
                new RLFeatureSpec("pos", NormalizerType.NONE)),
                TradeAction.class, PreprocessType.STANDARDIZE);
        RLModelMetadata rewritten = store.syncModel(new FakeStrategy("m", evolved));

        assertEquals(evolved.specHash(), rewritten.getSpecHash(), "可变字段应重写为现场 spec");
        assertEquals(3, rewritten.getStateDim());
        assertEquals(3, rewritten.getFeatures().size());
        assertEquals("Z_SCORE", rewritten.getFeatures().get(0).getNormalizer());
        assertEquals("standardize", rewritten.getPreprocessing().getType());
        assertEquals(1, rewritten.getLatestVersion(), "latest_version 必须保留");
        assertArrayEquals(new float[] {1, 2}, rewritten.getPreprocessing().getMean(), "预处理统计量必须保留");
        assertArrayEquals(new float[] {3, 4}, rewritten.getPreprocessing().getStd());
        // 重写落盘：重新加载仍为现场 spec
        assertEquals(evolved.specHash(),
                store.findModel("m").orElseThrow().getSpecHash());
    }

    @Test
    void syncModelTakesAlgoFromStrategyDeclaration() {
        RLModelMetadata meta = store.syncModel(new FakeStrategy("m", spec).withAlgo(RLAlgo.PPO));

        assertEquals(RLAlgo.PPO, meta.getAlgo(), "algo 应来自策略声明而非默认写死");
        assertEquals(RLAlgo.PPO, store.findModel("m").orElseThrow().getAlgo(), "落盘保留声明值");
    }

    @Test
    void syncModelRejectsAlgoChangeEvenWhenSpecUnchanged() {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> store.syncModel(strategy.withAlgo(RLAlgo.PPO)));
        assertTrue(e.getMessage().contains("DQN") && e.getMessage().contains("PPO"));
        assertEquals(RLAlgo.DQN, store.findModel("m").orElseThrow().getAlgo(), "失败时不改写已持久化的 algo");
    }

    @Test
    void syncModelBootstrapsCustomPreprocessingFromSpec() {
        RLModelSpec customSpec = new RLModelSpec(List.of(
                new RLFeatureSpec("ret1", NormalizerType.RATIO)),
                TradeAction.class, PreprocessType.CUSTOM);
        RLModelMetadata meta = store.syncModel(new FakeStrategy("m", customSpec).withPreprocessor(raw -> raw));

        assertEquals("custom", meta.getPreprocessing().getType());
        assertEquals("custom", store.findModel("m").orElseThrow().getPreprocessing().getType());
    }

    @Test
    void syncModelRejectsCustomPreprocessingWithoutPreprocessor() {
        RLModelSpec customSpec = new RLModelSpec(List.of(
                new RLFeatureSpec("ret1", NormalizerType.RATIO)),
                TradeAction.class, PreprocessType.CUSTOM);
        // FakeStrategy 默认 rlPreprocessor() 返回 null（模拟"未实现"）
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> store.syncModel(new FakeStrategy("m", customSpec)));
        assertTrue(e.getMessage().contains("rlPreprocessor"));
    }

    @Test
    void syncModelRejectsCustomPreprocessingWithThrowingDefaultMethod() {
        RLModelSpec customSpec = new RLModelSpec(List.of(
                new RLFeatureSpec("ret1", NormalizerType.RATIO)),
                TradeAction.class, PreprocessType.CUSTOM);
        // 沿用接口默认行为（抛 UnsupportedOperationException）的策略
        RLStrategy strategy = new FakeStrategy("m", customSpec) {
            @Override
            public RLStatePreprocessor rlPreprocessor() {
                throw new UnsupportedOperationException("rlSpec().preprocessing() 声明为 CUSTOM 时必须实现 rlPreprocessor()");
            }
        };
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> store.syncModel(strategy));
        assertTrue(e.getMessage().contains("未实现"));
    }

    /* ---------- fork / delete ---------- */

    @Test
    void forkFromScratchCreatesV1() {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);
        Map<String, Double> hp = new LinkedHashMap<>();
        hp.put("lr", 0.0005);
        RLVersionMetadata v1 = store.forkVersion(strategy, 0, false, hp, training());

        assertEquals(1, v1.getVersion());
        assertEquals(0, v1.getParentVersion());
        assertFalse(v1.isWeightInherited());
        assertEquals(spec.specHash(), v1.getSpecHash());
        assertEquals(hp, v1.getTraining().getHyperParams());
        assertTrue(Files.isDirectory(store.roundsDir("m", 1)));
        assertFalse(Files.exists(store.checkpointFile("m", 1)), "从零训练不应复制 checkpoint");
        assertEquals(1, store.findModel("m").orElseThrow().getLatestVersion());
    }

    @Test
    void forkWithInheritanceCopiesCheckpointAndPreprocessing() throws Exception {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);
        RLVersionMetadata v1 = store.forkVersion(strategy, 0, false, Map.of("lr", 0.001), training());
        // 模拟 Python 训练产物：checkpoint + 冻结的预处理统计量
        Files.writeString(store.checkpointFile("m", 1), "fake-checkpoint");
        v1.getPreprocessing().setType("standardize");
        v1.getPreprocessing().setMean(new float[] {1, 2});
        v1.getPreprocessing().setStd(new float[] {3, 4});
        v1.save(store.versionDir("m", 1));

        RLVersionMetadata v2 = store.forkVersion(strategy, 1, true, Map.of("lr", 0.0005), training());
        assertEquals(2, v2.getVersion());
        assertEquals(1, v2.getParentVersion());
        assertTrue(v2.isWeightInherited());
        assertEquals("fake-checkpoint", Files.readString(store.checkpointFile("m", 2)));
        assertArrayEquals(new float[] {1, 2}, v2.getPreprocessing().getMean(), "fork 应沿用父版本预处理统计量");
        assertEquals(2, store.findModel("m").orElseThrow().getLatestVersion());
    }

    @Test
    void forkRejectsMissingParentOrCheckpoint() {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);
        assertThrows(IllegalArgumentException.class,
                () -> store.forkVersion(strategy, 5, true, Map.of(), training()));

        store.forkVersion(strategy, 0, false, Map.of(), training());
        assertThrows(IllegalStateException.class,
                () -> store.forkVersion(strategy, 1, true, Map.of(), training()),
                "父版本无 checkpoint 时拒绝权重继承");
    }

    @Test
    void forkRejectsParentSpecHashMismatch() throws Exception {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);
        RLVersionMetadata v1 = store.forkVersion(strategy, 0, false, Map.of(), training());
        // 篡改父版本 specHash（模拟策略演进后父版本与新现场规格不一致）
        v1.setSpecHash("stale-hash");
        v1.save(store.versionDir("m", 1));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> store.forkVersion(strategy, 1, false, Map.of(), training()));
        assertTrue(e.getMessage().contains("拒绝 fork"), e.getMessage());
        assertEquals(1, store.findModel("m").orElseThrow().getLatestVersion(), "拒绝 fork 不产生新版本");
        assertFalse(Files.isDirectory(store.versionDir("m", 2)));
    }

    @Test
    void explicitDegradeForkKeepsPreprocessingWithoutCheckpoint() throws Exception {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);
        RLVersionMetadata v1 = store.forkVersion(strategy, 0, false, Map.of(), training());
        Files.writeString(store.checkpointFile("m", 1), "fake-checkpoint");
        v1.getPreprocessing().setType("standardize");
        v1.getPreprocessing().setMean(new float[] {1, 2});
        v1.getPreprocessing().setStd(new float[] {3, 4});
        v1.save(store.versionDir("m", 1));

        // 显式降级：baseVersion=1 但 inheritWeights=false —— 不复制权重，但继承特征定义与预处理统计量
        RLVersionMetadata v2 = store.forkVersion(strategy, 1, false, Map.of("lr", 0.001), training());

        assertEquals(2, v2.getVersion());
        assertEquals(1, v2.getParentVersion());
        assertFalse(v2.isWeightInherited(), "显式降级：weight_inherited 必须为 false（语义诚实）");
        assertFalse(Files.exists(store.checkpointFile("m", 2)), "显式降级不复制 checkpoint");
        assertArrayEquals(new float[] {1, 2}, v2.getPreprocessing().getMean(), "降级仍沿用父版本预处理统计量");
        assertArrayEquals(new float[] {3, 4}, v2.getPreprocessing().getStd());
    }

    /* ---------- 旧文件兼容性 ---------- */

    @Test
    void legacyMetadataWithNetworkFieldLoadsWithoutError() throws Exception {
        // 旧版 metadata 含已移除的 network 字段：Gson 默认忽略未知字段，加载不报错
        Path dir = store.modelDir("legacy");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(RLModelMetadata.FILE_NAME),
                "{\"name\":\"legacy\",\"algo\":\"DQN\",\"state_dim\":2,\"action_dim\":3,"
                        + "\"actions\":[\"HOLD\",\"BUY\",\"SELL\"],"
                        + "\"network\":{\"hidden\":[128,128],\"activation\":\"relu\"},"
                        + "\"preprocessing\":{\"type\":\"identity\"}}");

        RLModelMetadata meta = store.findModel("legacy").orElseThrow();
        assertTrue(meta.getFeatures().isEmpty(), "旧 metadata 无 features 字段按空列表处理");
    }

    @Test
    void diffSpecReportsStructuredDifferences() {
        RLModelMetadata meta = store.syncModel(new FakeStrategy("m", spec));
        assertTrue(meta.diffSpec(spec).isEmpty(), "与创建规格一致时应无差异");

        RLModelSpec evolved = new RLModelSpec(List.of(
                new RLFeatureSpec("ret1", NormalizerType.Z_SCORE),
                new RLFeatureSpec("maDev", NormalizerType.RATIO),
                new RLFeatureSpec("pos", NormalizerType.NONE)),
                TradeAction.class, PreprocessType.STANDARDIZE);
        List<String> diffs = meta.diffSpec(evolved);
        assertTrue(diffs.stream().anyMatch(d -> d.contains("特征[0]变更")), () -> "实际差异: " + diffs);
        assertTrue(diffs.stream().anyMatch(d -> d.contains("特征[2]新增")), () -> "实际差异: " + diffs);
        assertTrue(diffs.stream().anyMatch(d -> d.contains("预处理变更")), () -> "实际差异: " + diffs);
        assertFalse(diffs.stream().anyMatch(d -> d.contains("动作表变更")));
    }

    /* ---------- listModels：目录存在即收录，缺失可重建 ---------- */

    @Test
    void listModelsAndVersions() {
        FakeStrategy strategyA = new FakeStrategy("a", spec);
        FakeStrategy strategyB = new FakeStrategy("b", spec);
        store.syncModel(strategyA);
        store.syncModel(strategyB);
        store.forkVersion(strategyA, 0, false, Map.of(), training());
        store.forkVersion(strategyA, 0, false, Map.of(), training());	// v2 同样从零训练（parentVersion=0 表示不继承）

        assertEquals(2, store.listModels().size());
        assertEquals(List.of(1, 2), store.listVersions("a").stream().map(RLVersionMetadata::getVersion).toList());
    }

    @Test
    void listModelsRebuildsDirWithRegisteredStrategy() {
        // 目录存在但 metadata.json 缺失：目录名与注册策略 modelName 匹配 → 现场重建收录
        FakeStrategy strategy = new FakeStrategy("dqn-demo", spec);
        RLModelStore storeWithStrategies = new RLModelStore(root, List.of(strategy));
        assertDoesNotThrow(() -> Files.createDirectories(storeWithStrategies.modelDir("dqn-demo")));

        List<RLModelMetadata> models = storeWithStrategies.listModels();
        assertEquals(1, models.size());
        assertEquals(spec.specHash(), models.get(0).getSpecHash());
        assertTrue(Files.isRegularFile(storeWithStrategies.modelDir("dqn-demo").resolve(RLModelMetadata.FILE_NAME)),
                "重建应写出 metadata.json");
    }

    @Test
    void listModelsBootstrapsRegisteredStrategyWithoutDirectory() {
        // 目录尚不存在：已注册策略声明的模型也应收录（现场 bootstrap），前端随时有候选模型
        FakeStrategy strategy = new FakeStrategy("dqn-demo", spec);
        RLModelStore storeWithStrategies = new RLModelStore(root, List.of(strategy));
        assertFalse(Files.exists(storeWithStrategies.modelDir("dqn-demo")));

        List<RLModelMetadata> models = storeWithStrategies.listModels();
        assertEquals(1, models.size());
        assertEquals(0, models.get(0).getLatestVersion());
        assertTrue(Files.isRegularFile(storeWithStrategies.modelDir("dqn-demo").resolve(RLModelMetadata.FILE_NAME)),
                "bootstrap 应写出 metadata.json");
    }

    @Test
    void listModelsSkipsDirWithoutMatchingStrategy() throws Exception {
        // cartpole 场景：无 metadata.json 且无同名注册策略 → 跳过收录
        Files.createDirectories(store.modelDir("cartpole"));
        assertTrue(store.listModels().isEmpty());
    }

    @Test
    void listModelsRebuildsCorruptMetadataWithRegisteredStrategy() throws Exception {
        FakeStrategy strategy = new FakeStrategy("dqn-demo", spec);
        RLModelStore storeWithStrategies = new RLModelStore(root, List.of(strategy));
        Files.createDirectories(storeWithStrategies.modelDir("dqn-demo"));
        Files.writeString(storeWithStrategies.modelDir("dqn-demo").resolve(RLModelMetadata.FILE_NAME), "{corrupt");

        List<RLModelMetadata> models = storeWithStrategies.listModels();
        assertEquals(1, models.size(), "损坏的 metadata 应按现场 spec 重建收录");
        assertEquals(spec.specHash(), models.get(0).getSpecHash());
    }

    /* ---------- deleteVersion ---------- */

    @Test
    void deleteVersionRollsBackLatestAndRemovesDir() throws Exception {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);
        store.forkVersion(strategy, 0, false, Map.of("lr", 0.001), training());
        Files.writeString(store.checkpointFile("m", 1), "fake-checkpoint");

        store.deleteVersion("m", 1);

        assertFalse(Files.exists(store.versionDir("m", 1)), "版本目录应删除");
        assertEquals(0, store.findModel("m").orElseThrow().getLatestVersion(), "latest_version 应回退");
        assertTrue(store.listVersions("m").isEmpty());
    }

    @Test
    void deleteNonLatestVersionKeepsLatestPointer() {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);
        store.forkVersion(strategy, 0, false, Map.of(), training());
        store.forkVersion(strategy, 0, false, Map.of(), training());
        store.forkVersion(strategy, 0, false, Map.of(), training());

        store.deleteVersion("m", 2);	// 非 LIFO 回收中间版本：目录删除，latest_version 不变
        assertEquals(List.of(1, 3), store.listVersions("m").stream().map(RLVersionMetadata::getVersion).toList());
        assertEquals(3, store.findModel("m").orElseThrow().getLatestVersion());

        store.deleteVersion("m", 3);	// 回收最新版本：回退到剩余最大版本号（v2 已删，为 v1）
        assertEquals(List.of(1), store.listVersions("m").stream().map(RLVersionMetadata::getVersion).toList());
        assertEquals(1, store.findModel("m").orElseThrow().getLatestVersion());

        store.deleteVersion("m", 1);
        assertEquals(0, store.findModel("m").orElseThrow().getLatestVersion());
    }

    @Test
    void deleteVersionToleratesMissingModelMetadata() throws Exception {
        FakeStrategy strategy = new FakeStrategy("m", spec);
        store.syncModel(strategy);
        store.forkVersion(strategy, 0, false, Map.of(), training());
        Files.delete(store.modelDir("m").resolve(RLModelMetadata.FILE_NAME));

        assertDoesNotThrow(() -> store.deleteVersion("m", 1), "metadata 缺失不得阻塞版本回收");
        assertFalse(Files.exists(store.versionDir("m", 1)));
    }
}
