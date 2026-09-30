package org.dromara.northstar.rl.example;

import org.dromara.northstar.rl.agent.DqnAgent;
import org.dromara.northstar.rl.env.CartPoleEnv;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * DQN 推理 Demo：
 * 用 {@link DqnAgent} 从 model 目录（含 metadata.json + 模型文件）加载并初始化，
 * 在 Java 复刻的 CartPole 环境中以贪心策略跑完整回合。
 *
 * 接入新场景只需：一个自描述的 model 目录 + 一个与 metadata 动作表一致的动作枚举。
 */
public class DqnInference {

    /** CartPole 动作空间。枚举 ordinal/名称必须与 metadata.json 的 actions 表一致。 */
    enum CartPoleAction {
        LEFT, RIGHT
    }

    private static final Path MODEL_DIR = Path.of("models/cartpole");
    private static final int EPISODES = 5;

    public static void main(String[] args) throws Exception {
        // 从 model 目录初始化：维度、预处理策略、模型文件路径全部来自 metadata.json，
        // 并校验 CartPoleAction 枚举与 metadata 动作表一致。
        try (DqnAgent<CartPoleAction> agent = DqnAgent.fromModelDir(MODEL_DIR, CartPoleAction.class)) {
            System.out.println("模型加载成功: " + MODEL_DIR.toAbsolutePath()
                    + " (inputDim=" + agent.getInputDim() + ", actionDim=" + agent.getActionDim() + ")");

            // ---- 单次推理演示：打印 Q 值 ----
            float[] sampleState = {0.0f, 0.0f, 0.1f, 0.0f};
            float[] q = agent.qValues(sampleState);
            System.out.printf("单次推理示例 state=[0, 0, 0.1, 0] -> Q=%s, 动作=%s%n",
                    Arrays.toString(q), agent.act(sampleState));

            // ---- 完整回合演示：贪心策略跑 CartPole ----
            for (int ep = 1; ep <= EPISODES; ep++) {
                CartPoleEnv env = new CartPoleEnv(100 + ep);
                double[] state = env.reset();
                while (!env.isDone()) {
                    CartPoleAction action = agent.act(toFloat(state));
                    state = env.step(action.ordinal());
                }
                System.out.printf("Episode %d: 存活 %d 步（满分 %d）%n",
                        ep, env.getSteps(), CartPoleEnv.MAX_STEPS);
            }
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
