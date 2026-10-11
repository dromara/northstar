package org.dromara.northstar.rl.training;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Map;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.strategy.rl.RLAlgo;
import org.junit.jupiter.api.Test;

class DqnHyperParamsTest {

    @Test
    void metaInfoExposesAllFieldsForDynamicForm() {
        Collection<ComponentField> fields = new DqnHyperParams().getMetaInfo();
        assertEquals(10, fields.size(), "DQN 超参数字段清单");
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("lr")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("epsilonStart")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("maxGradSteps")));
    }

    @Test
    void epsilonDecaysLinearlyToFloor() {
        DqnHyperParams hp = new DqnHyperParams();	// 1.0 -> 0.05 over 15 rounds
        assertEquals(1.0, hp.epsilonAt(0), 1e-9);
        assertEquals(0.05, hp.epsilonAt(15), 1e-9);
        assertEquals(0.05, hp.epsilonAt(100), 1e-9, "衰减到底后保持下限");
        double mid = hp.epsilonAt(7);
        assertTrue(mid > 0.05 && mid < 1.0);
    }

    @Test
    void gradStepsFollowsNewSamplesWithBounds() {
        DqnHyperParams hp = new DqnHyperParams();
        assertEquals(300, hp.gradSteps(10), "梯度步数有下限");
        assertEquals(1500, hp.gradSteps(1500));
        assertEquals(2000, hp.gradSteps(999999), "梯度步数封顶 maxGradSteps");
    }

    @Test
    void defaultsAreExposedAsMap() {
        Map<String, Double> defaults = RLAlgorithms.hyperParamDefaults(RLAlgo.DQN);
        assertEquals(5e-4, defaults.get("lr"), 1e-12);
        assertEquals(0.0, defaults.get("gamma"), 1e-12, "γ 默认 0 = 自动（τ 对齐单局 bar 数）");
        assertEquals(128, defaults.get("batchSize"), 1e-12);
    }

    @Test
    void integerFieldsAreMarkedAsIntegerType() {
        Collection<ComponentField> fields = new DqnHyperParams().getMetaInfo();
        assertEquals(FieldType.INTEGER, typeOf(fields, "batchSize"));
        assertEquals(FieldType.INTEGER, typeOf(fields, "bufferCapacity"));
        assertEquals(FieldType.INTEGER, typeOf(fields, "minBuffer"));
        assertEquals(FieldType.INTEGER, typeOf(fields, "maxGradSteps"));
        assertEquals(FieldType.INTEGER, typeOf(fields, "epsilonDecayRounds"));
        assertEquals(FieldType.NUMBER, typeOf(fields, "lr"));
        assertEquals(FieldType.NUMBER, typeOf(fields, "gamma"));
    }

    @Test
    void doubleValuesFromStorageRoundTripIntoIntegerFields() throws Exception {
        // 模拟训练运行时把 Map<String, Double> 的取值（如 128.0）回填进 int 字段
        DqnHyperParams hp = new DqnHyperParams();
        Collection<ComponentField> fields = hp.getMetaInfo();
        fields.stream().filter(f -> f.getName().equals("batchSize")).forEach(f -> f.setValue(256.0D));
        fields.stream().filter(f -> f.getName().equals("maxGradSteps")).forEach(f -> f.setValue(3000.0D));
        hp.resolveFromSource(fields);
        assertEquals(256, hp.getBatchSize());
        assertEquals(3000, hp.getMaxGradSteps());
    }

    private static FieldType typeOf(Collection<ComponentField> fields, String name) {
        return fields.stream().filter(f -> f.getName().equals(name)).findFirst().orElseThrow().getType();
    }

    @Test
    void ppoAlgoFailsFastUntilV2() {
        assertThrows(UnsupportedOperationException.class, () -> RLAlgorithms.hyperParamsOf(RLAlgo.PPO));
    }
}
