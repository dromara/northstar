package org.dromara.northstar.rl.strategy;

import static org.junit.jupiter.api.Assertions.*;

import org.dromara.northstar.strategy.rl.RLModelSpec;
import org.junit.jupiter.api.Test;

/**
 * 策略演进防护的 golden 测试（设计文档 §4.1）：
 * 固定 rlSpec 的 specHash，任何对特征清单/动作表/预处理的无意识改动都会在此暴露。
 * 若确为有意变更：创建新模型并更新种子 metadata，而不是修改本测试的值。
 */
class RlTrendStrategyGoldenTest {

    /** 与 models/rl-trend-dqn/metadata.json 的 spec_hash 一致 */
    private static final String PINNED_SPEC_HASH = "91de2fdfbc70a327";

    @Test
    void specHashIsPinned() {
        RLModelSpec spec = new RlTrendStrategy().rlSpec();
        assertEquals(PINNED_SPEC_HASH, spec.specHash(),
                "rlSpec 发生变更：若属有意变更请创建新模型；若为无意变更请回退策略代码");
        assertEquals(6, spec.stateDim());
        assertEquals(3, spec.actionDim());
    }

    @Test
    void modelNameDeclaresNetworkIdentity() {
        assertEquals("rl-trend-dqn", new RlTrendStrategy().modelName(),
                "模型名即网络结构身份，须与 Python registry.py 的注册键一致");
    }
}
