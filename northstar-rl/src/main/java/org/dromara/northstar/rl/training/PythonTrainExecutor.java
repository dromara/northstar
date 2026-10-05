package org.dromara.northstar.rl.training;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * Python 单轮离线训练执行器：调用 training/train_round.py 子进程。
 *
 * Python 侧通过 stdout 输出结构化结果（LOSS=... TRAINED_STEPS=... BUFFER_SIZE=...），
 * 本类负责解析；stderr 直通日志。
 */
@Slf4j
public class PythonTrainExecutor implements RLTrainExecutor {

	private final String python;		// Python 解释器路径
	private final Path trainingDir;		// train_round.py 所在目录
	private final long timeoutMinutes;

	public PythonTrainExecutor(String python, Path trainingDir) {
		this(python, trainingDir, 30);
	}

	public PythonTrainExecutor(String python, Path trainingDir, long timeoutMinutes) {
		this.python = python;
		this.trainingDir = trainingDir;
		this.timeoutMinutes = timeoutMinutes;
	}

	@Override
	public RLTrainResult trainRound(Path modelDir, int version, int gradSteps, Double lrOverride, long seed)
			throws IOException, InterruptedException {
		if (!Files.isRegularFile(trainingDir.resolve("train_round.py"))) {
			throw new IllegalStateException("训练脚本不存在: " + trainingDir.resolve("train_round.py").toAbsolutePath());
		}
		ProcessBuilder pb = new ProcessBuilder(python, "train_round.py",
				"--model-dir", modelDir.toAbsolutePath().toString(),
				"--version", String.valueOf(version),
				"--grad-steps", String.valueOf(gradSteps),
				"--seed", String.valueOf(seed));
		if (lrOverride != null) {
			pb.command().addAll(java.util.List.of("--lr", String.valueOf(lrOverride)));
		}
		pb.directory(trainingDir.toFile());
		pb.environment().put("PYTHONUTF8", "1");	// Windows 中文系统默认 GBK，强制 UTF-8
		pb.redirectErrorStream(false);
		Process process = pb.start();
		StringBuilder stderrBuf = new StringBuilder();
		Thread stderrReader = new Thread(() -> {
			try (BufferedReader r = new BufferedReader(
					new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = r.readLine()) != null) {
					stderrBuf.append(line).append('\n');
				}
			} catch (IOException ignore) {
				// 进程结束时流被关闭，忽略
			}
		});
		stderrReader.setDaemon(true);
		stderrReader.start();

		StdoutResult acc = new StdoutResult();
		try (BufferedReader stdout = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = stdout.readLine()) != null) {
				log.debug("[train_round] {}", line);
				parseLine(line, acc);
			}
		}
		if (!process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
			process.destroyForcibly();
			throw new IllegalStateException("Python 训练超时（" + timeoutMinutes + " 分钟）");
		}
		int exit = process.exitValue();
		if (exit != 0) {
			throw new IllegalStateException("Python 训练失败，退出码 " + exit + "，stderr: " + stderrBuf);
		}
		return new RLTrainResult(acc.loss, acc.trainedSteps, acc.bufferSize);
	}

	/** stdout 结构化输出（LOSS/TRAINED_STEPS/BUFFER_SIZE）的累积解析结果 */
	static class StdoutResult {
		double loss = Double.NaN;
		int trainedSteps;
		int bufferSize;
	}

	/**
	 * 解析一行 stdout 中的 KEY=VALUE token（忽略无关 token 与畸形片段）；
	 * LOSS 取值 "nan" 表示该轮无有效梯度更新（如经验池样本不足），按 NaN 透传。
	 */
	static void parseLine(String line, StdoutResult acc) {
		for (String token : line.trim().split("\\s+")) {
			String[] kv = token.split("=", 2);
			if (kv.length != 2) {
				continue;
			}
			switch (kv[0]) {
				case "LOSS" -> acc.loss = "nan".equalsIgnoreCase(kv[1]) ? Double.NaN : Double.parseDouble(kv[1]);
				case "TRAINED_STEPS" -> acc.trainedSteps = Integer.parseInt(kv[1]);
				case "BUFFER_SIZE" -> acc.bufferSize = Integer.parseInt(kv[1]);
				default -> { }
			}
		}
	}
}
