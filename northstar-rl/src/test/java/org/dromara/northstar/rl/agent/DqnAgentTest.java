package org.dromara.northstar.rl.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ai.djl.translate.Translator;

/**
 * DqnAgent 的保守验证：构造期/加载期的快速失败校验与预处理钩子。
 * 全部用例在模型文件加载之前触发异常，无需 PyTorch 原生库。
 */
class DqnAgentTest {

    enum TwoActions { LEFT, RIGHT }

    enum ThreeActions { LEFT, RIGHT, KEEP }

    @Test
    void rejectsNonPositiveDims() {
        Path dummy = Path.of("no-such-model.pt");
        assertThrows(IllegalArgumentException.class,
                () -> new DqnAgent<>(dummy, 0, 2, TwoActions.class));
        assertThrows(IllegalArgumentException.class,
                () -> new DqnAgent<>(dummy, 4, -1, TwoActions.class));
    }

    @Test
    void rejectsEnumCountMismatchBeforeModelLoading() {
        // 校验发生在模型加载之前，因此传不存在的模型路径也应抛出参数异常
        Path dummy = Path.of("no-such-model.pt");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DqnAgent<>(dummy, 4, 2, ThreeActions.class));
        assertTrue(e.getMessage().contains("actionDim"));
    }

    @Test
    void fromModelDirRejectsActionTableMismatch(@TempDir Path dir) throws Exception {
        // 动作表数量与枚举不一致
        Files.writeString(dir.resolve(ModelMetadata.FILE_NAME), """
                {"state_dim":4,"action_dim":3,"actions":["LEFT","RIGHT","KEEP"]}
                """);
        assertThrows(IllegalArgumentException.class,
                () -> DqnAgent.fromModelDir(dir, TwoActions.class));

        // 数量一致但名称/顺序不一致
        Files.writeString(dir.resolve(ModelMetadata.FILE_NAME), """
                {"state_dim":4,"action_dim":2,"actions":["RIGHT","LEFT"]}
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DqnAgent.fromModelDir(dir, TwoActions.class));
        assertTrue(e.getMessage().contains("动作下标 0"));
    }

    @Test
    void fromModelDirRejectsUnknownPreprocessingType(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve(ModelMetadata.FILE_NAME), """
                {"state_dim":4,"action_dim":2,"actions":["LEFT","RIGHT"],
                 "preprocessing":{"type":"minmax"}}
                """);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DqnAgent.fromModelDir(dir, TwoActions.class));
        assertTrue(e.getMessage().contains("未知的预处理类型"));
    }

    @Test
    void standardizingRejectsInconsistentParams() {
        assertThrows(IllegalArgumentException.class,
                () -> DqnAgent.standardizing(new float[] {1, 2}, new float[] {1}));
    }

    @Test
    void standardizingPreprocessNormalizesInput() {
        Translator<float[], float[]> t = DqnAgent.standardizing(
                new float[] {10f, 20f}, new float[] {2f, 4f});
        // 与主代码同包，可直接调用 protected 预处理钩子
        float[] out = ((DqnAgent.FloatVectorTranslator) t).preprocess(new float[] {12f, 28f});
        assertArrayEquals(new float[] {1f, 2f}, out, 1e-6f);
        assertThrows(IllegalArgumentException.class,
                () -> ((DqnAgent.FloatVectorTranslator) t).preprocess(new float[] {1f}));
    }

    @Test
    void identityTranslatorPassesThrough() {
        DqnAgent.FloatVectorTranslator t = new DqnAgent.FloatVectorTranslator();
        float[] raw = {0.1f, -0.2f, 3f};
        assertArrayEquals(raw, t.preprocess(raw));
    }
}
