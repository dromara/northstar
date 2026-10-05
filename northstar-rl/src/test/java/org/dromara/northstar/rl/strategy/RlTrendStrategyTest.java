package org.dromara.northstar.rl.strategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.rl.model.RLModelMetadata;
import org.dromara.northstar.rl.model.RLModelStore;
import org.dromara.northstar.rl.model.RLVersionMetadata;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.rl.EpisodeEnd;
import org.dromara.northstar.strategy.rl.RLRewardContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * RlTrendStrategy 单测：reward 重罚分支、实盘推理加载的 specHash 防护、observe 特征向量 golden 快照。
 */
class RlTrendStrategyTest {

    @TempDir
    Path modelsRoot;

    /* ---------- reward：TERMINATED 重罚分支 ---------- */

    @Test
    void rewardPenalizesTerminatedHeavily() {
        RlTrendStrategy strategy = new RlTrendStrategy();
        // 区间亏损 1%：TERMINATED = -1 - 10 = -11
        assertEquals(-11.0, strategy.reward(new RLRewardContext(100_000, 99_000, List.of(), 5, EpisodeEnd.TERMINATED)), 1e-9,
                "权益击穿的真失败应施加重罚");
        // 相同区间亏损：TRUNCATED 不罚
        assertEquals(-1.0, strategy.reward(new RLRewardContext(100_000, 99_000, List.of(), 5, EpisodeEnd.TRUNCATED)), 1e-9);
        // 普通决策点：区间收益（百分点）即奖励
        assertEquals(1.0, strategy.reward(new RLRewardContext(100_000, 101_000, List.of(), 3, EpisodeEnd.NONE)), 1e-9);
    }

    /* ---------- liveAgent：specHash 不匹配失败路径 ---------- */

    @Test
    void liveAgentRejectsSpecHashMismatchWithStructuredDiff() throws Exception {
        // 构造与策略不匹配的模型目录：specHash 与特征清单均被篡改（模拟策略无意识演进后的旧模型）
        RLModelStore store = new RLModelStore(modelsRoot);
        store.syncModel(new RlTrendStrategy());
        RLModelMetadata meta = store.findModel("rl-trend-dqn").orElseThrow();
        meta.setSpecHash("stale-hash");
        List<RLModelMetadata.FeatureInfo> features = new ArrayList<>(meta.getFeatures());
        features.set(0, new RLModelMetadata.FeatureInfo("ret1", "Z_SCORE"));
        meta.setFeatures(features);
        meta.save(modelsRoot.resolve("rl-trend-dqn"));
        Path versionDir = modelsRoot.resolve("rl-trend-dqn/versions/v1");
        Files.createDirectories(versionDir);
        RLVersionMetadata verMeta = new RLVersionMetadata();
        verMeta.setVersion(1);
        verMeta.setSpecHash("stale-hash");
        verMeta.save(versionDir);

        RlTrendStrategy strategy = new RlTrendStrategy();
        RlTrendStrategy.InitParams params = new RlTrendStrategy.InitParams();
        Field dirField = RlTrendStrategy.InitParams.class.getDeclaredField("modelVersionDir");
        dirField.setAccessible(true);
        dirField.set(params, versionDir.toString());
        strategy.initWithParams(params);

        Method liveAgent = RlTrendStrategy.class.getDeclaredMethod("liveAgent");
        liveAgent.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> liveAgent.invoke(strategy));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("specHash"), ex.getCause().getMessage());
        assertTrue(ex.getCause().getMessage().contains("特征[0]变更"),
                "失败信息应包含结构化 diff: " + ex.getCause().getMessage());
    }

    /* ---------- observe golden 快照 ---------- */

    @Test
    void observeGoldenSnapshot() {
        // golden 快照：以下数值由首次运行的真实输出钉入（空仓状态、固定价格序列）。
        // 变更特征计算逻辑（指标参数/公式/归一化）必须显式更新本 golden，并在评审中说明理由。
        Contract contract = Contract.builder()
                .contractId("rb0000@SHFE@FUTURES@PLAYBACK")
                .unifiedSymbol("rb0000@SHFE@FUTURES")
                .symbol("rb0000")
                .name("螺纹钢指数")
                .build();
        IModuleContext ctx = mock(IModuleContext.class);
        when(ctx.bindedContracts()).thenReturn(List.of(contract));
        when(ctx.getState()).thenReturn(ModuleState.EMPTY);
        List<Indicator> registered = new ArrayList<>();
        doAnswer(inv -> {
            registered.add(inv.getArgument(0));
            return null;
        }).when(ctx).registerIndicator(any());

        RlTrendStrategy strategy = new RlTrendStrategy();
        strategy.setContext(ctx);
        assertEquals(4, registered.size(), "maFast/maSlow/hhv/llv 四个注册指标");

        long baseTs = LocalDate.of(2024, 1, 2).atTime(LocalTime.of(9, 0))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        for (int i = 0; i < 80; i++) {
            double price = 3500 + i * 1.5 + 20 * Math.sin(i / 4.0);
            feedAll(registered, Num.of(price, baseTs + i * 60_000L));
        }

        float[] state = strategy.observe(ctx, contract);

        assertEquals(6, state.length);
        assertArrayEquals(new float[] {
                0.0013946282f,	// ret1
                0.008329749f,	// ret5
                0.009640334f,	// maDev
                0.0064859176f,	// trendDev
                1.0f,			// rangePos（序列单调上行末端，收盘价位于区间顶）
                0.0f			// position（空仓）
        }, state, 0f, () -> "observe 输出向量与 golden 不一致：若特征变更为有意为之，请显式更新 golden。实际值: "
                + java.util.Arrays.toString(state));
    }

    /** 与 IndicatorValueUpdateHelper 相同的递归更新语义：先依赖后自身，幂等（同一时间戳重复更新无效） */
    private static void feedAll(List<Indicator> indicators, Num num) {
        for (Indicator indicator : indicators) {
            recursiveUpdate(indicator, num);
        }
    }

    private static void recursiveUpdate(Indicator indicator, Num num) {
        indicator.dependencies().forEach(dep -> recursiveUpdate(dep, num));
        indicator.update(num);
    }
}
