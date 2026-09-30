package org.dromara.northstar.rl.training;

import org.dromara.northstar.rl.agent.ModelMetadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 一站式 DQN 训练流水线（Java 驱动，常驻进程）。
 *
 * 每轮循环：
 * <ol>
 *   <li>进程内调用 {@link ExperienceCollector#collect} ε-greedy 采样（首轮模型不存在时纯随机）</li>
 *   <li>调用 Python 单轮训练器 {@code train_round.py} 做离线回放训练并导出模型</li>
 *   <li>进程内调用 {@link ExperienceCollector#evaluate} 贪心评估，达标即停</li>
 * </ol>
 *
 * 跨语言边界只有一处：Java -> Python 的子进程调用。
 * Python 进程无状态，跨轮训练状态持久化在 model 目录（replay.csv + checkpoint.pt）。
 *
 * 用法：
 *   mvn compile exec:java -Dexec.mainClass=org.dromara.northstar.rl.training.TrainingPipeline
 * 可选参数：
 *   --model-dir <目录>   --rounds <n>   --episodes-per-round <n>   --eval-episodes <n>
 *   --accept <均分线>    --python <python解释器路径>   --seed <n>
 */
public class TrainingPipeline {

    private static final double EPS_START = 1.0;
    private static final double EPS_END = 0.05;
    private static final int EPS_DECAY_ROUNDS = 15;
    private static final int MAX_GRAD_STEPS = 2000;
    private static final int MIN_GRAD_STEPS = 300;
    private static final double MAX_SCORE = 500.0;
    private static final double LR_START = 5e-4;  // 随轮数线性衰减到 ~0，后期收敛防震荡

    public static void main(String[] args) throws Exception {
        Path modelDir = Path.of(option(args, "--model-dir", "models/cartpole"));
        int rounds = Integer.parseInt(option(args, "--rounds", "30"));
        int episodesPerRound = Integer.parseInt(option(args, "--episodes-per-round", "20"));
        int evalEpisodes = Integer.parseInt(option(args, "--eval-episodes", "10"));
        double accept = Double.parseDouble(option(args, "--accept", "475"));
        long seed = Long.parseLong(option(args, "--seed", "42"));
        // Python 解释器：默认使用项目 venv，找不到则回退到 PATH 中的 python
        Path python = Path.of(option(args, "--python", ".venv/Scripts/python.exe"));
        if (!Files.exists(python)) {
            python = Path.of("python");
        }

        ModelMetadata meta = ModelMetadata.load(modelDir);
        Path modelPath = modelDir.resolve(meta.getModelFile());
        Path dataFile = modelDir.resolve(meta.getDataFile());
        Path bestModel = modelDir.resolve("model_best.pt");

        // 从干净状态开始：清空经验数据与训练 checkpoint
        Files.deleteIfExists(dataFile);
        Files.deleteIfExists(modelDir.resolve("checkpoint.pt"));
        Files.deleteIfExists(bestModel);
        System.out.println("开始训练: modelDir=" + modelDir.toAbsolutePath()
                + " python=" + python + " rounds=" + rounds);

        double bestScore = -1;
        for (int r = 0; r < rounds; r++) {
            double epsilon = Math.max(EPS_END, EPS_START - (EPS_START - EPS_END) * r / EPS_DECAY_ROUNDS);

            // 1) 进程内采样（每步 = 一条转移样本）
            int newSamples = ExperienceCollector.collect(meta, modelDir, epsilon,
                    episodesPerRound, dataFile, seed + r);

            // 2) Python 离线训练：梯度步数与新增样本量挂钩（约 1:1 回放比），LR 线性衰减
            int gradSteps = Math.min(MAX_GRAD_STEPS, Math.max(MIN_GRAD_STEPS, newSamples));
            double lr = LR_START * (1.0 - (double) r / rounds);
            runPythonTrainRound(python, modelDir, gradSteps, lr, seed + r);

            // 3) 进程内贪心评估
            double score = ExperienceCollector.evaluate(modelDir, evalEpisodes, 999);
            System.out.printf("Round %2d | eps %.3f | lr %.5f | 新增样本 %5d | 梯度步 %4d | Java评估 %6.1f/%.0f%n",
                    r, epsilon, lr, newSamples, gradSteps, score, MAX_SCORE);

            if (score > bestScore) {
                bestScore = score;
                Files.copy(modelPath, bestModel, StandardCopyOption.REPLACE_EXISTING);
            }
            if (score >= accept) {
                System.out.printf("==> 达标！Java 侧贪心评估 %.1f >= %.1f%n", score, accept);
                break;
            }
        }

        // 恢复评估最优的模型，避免后期波动
        if (Files.exists(bestModel)) {
            Files.move(bestModel, modelPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("恢复最优模型 (Java评估=%.1f) 作为最终产物%n", bestScore);
        }
        System.out.println("训练完成，最终模型: " + modelPath.toAbsolutePath());
    }

    /** 调用 Python 单轮离线训练器，输出直通控制台。 */
    private static void runPythonTrainRound(Path python, Path modelDir, int gradSteps,
                                            double lr, long seed) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                python.toString(), "train_round.py",
                "--model-dir", modelDir.toAbsolutePath().toString(),
                "--grad-steps", String.valueOf(gradSteps),
                "--lr", String.valueOf(lr),
                "--seed", String.valueOf(seed));
        pb.directory(Path.of("training").toFile());
        pb.environment().put("PYTHONUTF8", "1");
        pb.inheritIO();
        int exit = pb.start().waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Python 训练失败，退出码 " + exit);
        }
    }

    private static String option(String[] args, String name, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}
