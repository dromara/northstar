package org.dromara.northstar.rl.example;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.dromara.northstar.rl.agent.DqnAgent;
import org.dromara.northstar.rl.env.CartPoleEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

/**
 * DqnAgent 推理示例调用（等价于 DqnInference main 方法的单测版本）。
 *
 * 该用例会真正加载 PyTorch 原生库与本模块 models/cartpole/versions/v1/model.pt，
 * 为保持默认单元测试的轻量与平台无关（pytorch-native-cpu 默认 win-x86_64），
 * 仅在显式指定 -Drl.inference.test=true 时执行，否则整类跳过。
 */
class DqnInferenceExampleTest {

    /** 与 models/cartpole/metadata.json 的 actions 表一致的动作枚举。 */
    enum CartPoleAction { LEFT, RIGHT }

    private static final Path VERSION_DIR = Path.of("models/cartpole/versions/v1");

    @BeforeAll
    static void assumeInferenceEnabled() {
        Assumptions.assumeTrue(Boolean.getBoolean("rl.inference.test"),
                "推理示例测试默认跳过，使用 -Drl.inference.test=true 显式开启");
    }

    @Test
    void agentLoadsAndDecidesFromModelDir() throws Exception {
        try (DqnAgent<CartPoleAction> agent = DqnAgent.fromVersionDir(VERSION_DIR, CartPoleAction.class)) {
            assertEquals(4, agent.getInputDim());
            assertEquals(2, agent.getActionDim());

            float[] q = agent.qValues(new float[] {0.0f, 0.0f, 0.1f, 0.0f});
            assertEquals(2, q.length);
            assertNotNull(agent.act(new float[] {0.0f, 0.0f, 0.1f, 0.0f}));
        }
    }

    @Test
    void greedyPolicyCompletesEpisode() throws Exception {
        try (DqnAgent<CartPoleAction> agent = DqnAgent.fromVersionDir(VERSION_DIR, CartPoleAction.class)) {
            CartPoleEnv env = new CartPoleEnv(101);
            double[] state = env.reset();
            while (!env.isDone()) {
                state = env.step(agent.act(toFloat(state)).ordinal());
            }
            assertTrue(env.getSteps() >= 1, "回合应至少存活 1 步");
            System.out.printf("贪心策略存活 %d 步（满分 %d）%n", env.getSteps(), CartPoleEnv.MAX_STEPS);
        }
    }

    private static float[] toFloat(double[] d) {
        float[] f = new float[d.length];
        for (int i = 0; i < d.length; i++) {
            f[i] = (float) d[i];
        }
        return f;
    }
}
