package org.dromara.northstar.rl.training;

import org.dromara.northstar.rl.agent.DqnAgent;
import org.dromara.northstar.rl.agent.ModelMetadata;
import org.dromara.northstar.rl.env.CartPoleEnv;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

/**
 * 经验采样器（behavior 端）：用当前模型以 ε-greedy 策略与环境交互，
 * 把转移样本 (s, a, r, s', terminated) 追加写入 CSV，供 Python 端离线回放训练。
 *
 * 用法：
 *   collect --model-dir <model目录> --epsilon <ε> --episodes <n> [--out <csv路径>] [--seed n]
 *   eval    --model-dir <model目录> --episodes <n> [--seed n]
 *
 * model 目录是自描述的：包含 metadata.json（维度/动作表/预处理）、模型文件与训练数据。
 * collect：模型文件不存在时使用纯随机策略（用于冷启动首轮采样），样本默认写入目录内
 * metadata 指定的 data_file。
 * eval：纯贪心评估，逐局打印存活步数，最后一行输出 AVG_STEPS=xxx 供训练脚本解析。
 */
public class ExperienceCollector {

    /** 与 metadata.json 的 actions 表一致的动作枚举：ordinal 即模型动作下标。 */
    enum CartPoleAction {
        LEFT, RIGHT
    }

    /** CSV 表头：与 Python 训练侧的数据契约一致。 */
    private static final String CSV_HEADER = "s0,s1,s2,s3,action,reward,ns0,ns1,ns2,ns3,terminated\n";

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path modelDir = Path.of(option(args, "--model-dir", "models/cartpole"));
        ModelMetadata meta = ModelMetadata.load(modelDir);
        Path modelPath = modelDir.resolve(meta.getModelFile());
        double epsilon = Double.parseDouble(option(args, "--epsilon", "0"));
        int episodes = Integer.parseInt(option(args, "--episodes", "20"));
        long seed = Long.parseLong(option(args, "--seed", "42"));

        switch (mode) {
            case "collect" -> {
                String outArg = option(args, "--out", null);
                Path out = outArg != null ? Path.of(outArg) : modelDir.resolve(meta.getDataFile());
                int steps = collect(meta, modelDir, epsilon, episodes, out, seed);
                System.out.println("COLLECTED_STEPS=" + steps);
            }
            case "eval" -> {
                double avg = evaluate(modelDir, episodes, seed);
                System.out.printf("AVG_STEPS=%.1f%n", avg);
            }
            default -> throw new IllegalArgumentException("未知模式: " + mode + "（支持 collect/eval）");
        }
    }

    /**
     * ε-greedy 采样，转移样本追加写入 CSV：s0..s3,a,r,ns0..ns3,terminated。
     *
     * @return 本轮采集的样本数（= 总步数）
     */
    public static int collect(ModelMetadata meta, Path modelDir, double epsilon,
                              int episodes, Path out, long seed) throws Exception {
        Path modelPath = modelDir.resolve(meta.getModelFile());
        boolean randomPolicy = !Files.exists(modelPath);
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        boolean writeHeader = !Files.exists(out) || Files.size(out) == 0; // 仅新文件写表头
        CartPoleEnv env = new CartPoleEnv(seed);
        Random rng = new Random(seed);

        try (BufferedWriter writer = Files.newBufferedWriter(out,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             DqnAgent<CartPoleAction> agent = randomPolicy
                     ? null
                     : DqnAgent.fromModelDir(modelDir, CartPoleAction.class)) {
            if (writeHeader) {
                writer.append(CSV_HEADER);
            }
            int totalSteps = 0;
            int actionDim = meta.getActionDim();
            for (int ep = 0; ep < episodes; ep++) {
                double[] state = env.reset();
                while (!env.isDone()) {
                    int action;
                    if (randomPolicy || rng.nextDouble() < epsilon) {
                        action = rng.nextInt(actionDim);            // 探索
                    } else {
                        action = agent.act(toFloat(state)).ordinal(); // 利用
                    }
                    double[] next = env.step(action);
                    // reward 恒为 1；done 只标记真正失败（越界/倒杆），500 步截断不算终止
                    writer.append(toRow(state, action, 1.0, next, env.isTerminated()));
                    state = next;
                    totalSteps++;
                }
            }
            return totalSteps;
        }
    }

    /**
     * 纯贪心评估。
     *
     * @return 平均存活步数
     */
    public static double evaluate(Path modelDir, int episodes, long seed) throws Exception {
        CartPoleEnv env = new CartPoleEnv(seed);
        try (DqnAgent<CartPoleAction> agent = DqnAgent.fromModelDir(modelDir, CartPoleAction.class)) {
            int total = 0;
            for (int ep = 1; ep <= episodes; ep++) {
                double[] state = env.reset();
                while (!env.isDone()) {
                    state = env.step(agent.act(toFloat(state)).ordinal());
                }
                total += env.getSteps();
            }
            return total / (double) episodes;
        }
    }

    private static String toRow(double[] s, int a, double r, double[] ns, boolean terminated) {
        StringBuilder sb = new StringBuilder();
        for (double v : s) {
            sb.append(v).append(',');
        }
        sb.append(a).append(',').append(r).append(',');
        for (double v : ns) {
            sb.append(v).append(',');
        }
        sb.append(terminated ? 1 : 0).append('\n');
        return sb.toString();
    }

    private static String option(String[] args, String name, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    private static float[] toFloat(double[] d) {
        float[] f = new float[d.length];
        for (int i = 0; i < d.length; i++) {
            f[i] = (float) d[i];
        }
        return f;
    }
}
