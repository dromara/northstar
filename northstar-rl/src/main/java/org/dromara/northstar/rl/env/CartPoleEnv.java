package org.dromara.northstar.rl.env;

import java.util.Random;

/**
 * CartPole-v1 环境的 Java 复刻（与 gymnasium 的 classic_control/cartpole.py 物理参数一致），
 * 用于在纯 Java 侧驱动 DQN 策略跑完整的 episode，验证推理效果。
 *
 * 观测空间: [x, x_dot, theta, theta_dot]（小车位置/速度、杆角度/角速度）
 * 动作空间: 0 = 向左推, 1 = 向右推
 */
public class CartPoleEnv {

    private static final double GRAVITY = 9.8;
    private static final double MASS_CART = 1.0;
    private static final double MASS_POLE = 0.1;
    private static final double TOTAL_MASS = MASS_POLE + MASS_CART;
    private static final double LENGTH = 0.5;          // 杆长的一半
    private static final double POLEMASS_LENGTH = MASS_POLE * LENGTH;
    private static final double FORCE_MAG = 10.0;
    private static final double TAU = 0.02;            // 步长（秒）
    private static final double THETA_THRESHOLD = 12 * 2 * Math.PI / 360; // 12 度
    private static final double X_THRESHOLD = 2.4;

    public static final int MAX_STEPS = 500;           // CartPole-v1 的单局步数上限

    private final Random random;
    private double[] state;                            // [x, xDot, theta, thetaDot]
    private int steps;

    public CartPoleEnv(long seed) {
        this.random = new Random(seed);
    }

    /** 重置环境，与 gym 一致：四个状态量都在 [-0.05, 0.05] 均匀采样。 */
    public double[] reset() {
        state = new double[4];
        for (int i = 0; i < 4; i++) {
            state[i] = random.nextDouble(-0.05, 0.05);
        }
        steps = 0;
        return state.clone();
    }

    /**
     * 执行一步。
     *
     * @return 新观测；通过 {@link #isDone()} 判断回合是否结束
     */
    public double[] step(int action) {
        double x = state[0];
        double xDot = state[1];
        double theta = state[2];
        double thetaDot = state[3];

        double force = (action == 1) ? FORCE_MAG : -FORCE_MAG;
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        double temp = (force + POLEMASS_LENGTH * thetaDot * thetaDot * sinTheta) / TOTAL_MASS;
        double thetaAcc = (GRAVITY * sinTheta - cosTheta * temp)
                / (LENGTH * (4.0 / 3.0 - MASS_POLE * cosTheta * cosTheta / TOTAL_MASS));
        double xAcc = temp - POLEMASS_LENGTH * thetaAcc * cosTheta / TOTAL_MASS;

        // Euler 积分（与 gym 默认一致）
        x = x + TAU * xDot;
        xDot = xDot + TAU * xAcc;
        theta = theta + TAU * thetaDot;
        thetaDot = thetaDot + TAU * thetaAcc;

        state = new double[] {x, xDot, theta, thetaDot};
        steps++;
        return state.clone();
    }

    /** 回合结束 = 越界/倒杆（terminated）或达到步数上限（truncated）。 */
    public boolean isDone() {
        return isTerminated() || steps >= MAX_STEPS;
    }

    /** 真正失败（越界/倒杆）。训练数据的 done 标志必须以此为准，步数截断不算终止。 */
    public boolean isTerminated() {
        double x = state[0];
        double theta = state[2];
        return x < -X_THRESHOLD || x > X_THRESHOLD
                || theta < -THETA_THRESHOLD || theta > THETA_THRESHOLD;
    }

    public int getSteps() {
        return steps;
    }
}
