package org.dromara.northstar.rl.env;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * CartPoleEnv 环境示例的保守验证：初始观测范围、确定性、步进与终止判定。
 * 不涉及任何模型推理，无需 PyTorch 原生库。
 */
class CartPoleEnvTest {

    @Test
    void resetReturnsStateWithinGymRanges() {
        CartPoleEnv env = new CartPoleEnv(42);
        for (int i = 0; i < 100; i++) {
            double[] state = env.reset();
            assertEquals(4, state.length);
            for (double v : state) {
                assertTrue(v >= -0.05 && v <= 0.05, "初始观测超出 [-0.05, 0.05]: " + v);
            }
            assertFalse(env.isDone());
            assertEquals(0, env.getSteps());
        }
    }

    @Test
    void sameSeedProducesSameTrajectory() {
        CartPoleEnv env1 = new CartPoleEnv(7);
        CartPoleEnv env2 = new CartPoleEnv(7);
        assertArrayEquals(env1.reset(), env2.reset());
        for (int i = 0; i < 20; i++) {
            assertArrayEquals(env1.step(i % 2), env2.step(i % 2));
        }
    }

    @Test
    void stepAdvancesStateAndStepCount() {
        CartPoleEnv env = new CartPoleEnv(1);
        double[] s0 = env.reset();
        double[] s1 = env.step(1);
        assertEquals(1, env.getSteps());
        assertFalse(java.util.Arrays.equals(s0, s1), "步进后状态应发生变化");
    }

    @Test
    void alwaysPushRightTerminatesBeforeMaxSteps() {
        // 恒定向右推的极端策略必然倒杆/越界，验证 terminated 判定生效
        CartPoleEnv env = new CartPoleEnv(42);
        env.reset();
        while (!env.isDone()) {
            env.step(1);
        }
        assertTrue(env.isTerminated(), "极端策略应真正失败而非步数截断");
        assertTrue(env.getSteps() < CartPoleEnv.MAX_STEPS);
    }

    @Test
    void stepNeverExceedsMaxStepsWhenDone() {
        // 收敛策略不可用时，至少验证 isDone 在达到上限后为 true（通过交替动作尽量延长回合）
        CartPoleEnv env = new CartPoleEnv(3);
        env.reset();
        int guard = 0;
        while (!env.isDone() && guard++ < CartPoleEnv.MAX_STEPS + 10) {
            env.step(env.getSteps() % 2);
        }
        assertTrue(env.getSteps() <= CartPoleEnv.MAX_STEPS);
    }
}
