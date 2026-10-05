package org.dromara.northstar.rl.experience;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * 经验数据仓库（任务级共享）：一个训练任务训练的是同一个 model，
 * 各合约环境产生的 transition 汇入同一份经验集（按轮次 × 合约分片落盘）。
 *
 * CSV 契约（与 Python 训练侧一致，按 stateDim 通用切分）：
 * <pre>s0,...,s{stateDim-1},action,reward,ns0,...,ns{stateDim-1},dt,terminated</pre>
 *
 * 分片文件：{roundsDir}/replay-r{round}-c{contractIdx}.csv，每轮每合约一个文件，
 * Python 侧增量读取（持久化 replay buffer，只读新增分片）。
 */
public class ExperienceStore implements AutoCloseable {

	private final Path roundsDir;
	private final int stateDim;
	private final Map<String, BufferedWriter> writers = new HashMap<>();

	public ExperienceStore(Path roundsDir, int stateDim) {
		this.roundsDir = roundsDir;
		this.stateDim = stateDim;
		try {
			Files.createDirectories(roundsDir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String header(int stateDim) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < stateDim; i++) {
			sb.append('s').append(i).append(',');
		}
		sb.append("action,reward,");
		for (int i = 0; i < stateDim; i++) {
			sb.append("ns").append(i).append(',');
		}
		sb.append("dt,terminated");
		return sb.toString();
	}

	/** 追加一条转移样本到指定轮次 × 合约的分片 */
	public void append(int round, int contractIdx, Transition t) {
		if (t.state().length != stateDim || t.nextState().length != stateDim) {
			throw new IllegalArgumentException("状态维度与 stateDim=" + stateDim + " 不一致");
		}
		try {
			writer(round, contractIdx).append(toRow(t)).append('\n');
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** 刷盘并关闭当前所有分片 writer（每轮采样结束后调用，保证 Python 读到完整数据） */
	public void flush() {
		writers.values().forEach(w -> {
			try {
				w.flush();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Override
	public void close() {
		writers.values().forEach(w -> {
			try {
				w.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		writers.clear();
	}

	public Path shardPath(int round, int contractIdx) {
		return roundsDir.resolve("replay-r%d-c%d.csv".formatted(round, contractIdx));
	}

	private BufferedWriter writer(int round, int contractIdx) throws IOException {
		String key = round + "-" + contractIdx;
		BufferedWriter w = writers.get(key);
		if (w == null) {
			Path path = shardPath(round, contractIdx);
			boolean writeHeader = !Files.exists(path) || Files.size(path) == 0;
			w = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			if (writeHeader) {
				w.append(header(stateDim)).append('\n');
			}
			writers.put(key, w);
		}
		return w;
	}

	private String toRow(Transition t) {
		StringBuilder sb = new StringBuilder();
		for (float v : t.state()) {
			sb.append(v).append(',');
		}
		sb.append(t.action()).append(',').append(t.reward()).append(',');
		for (float v : t.nextState()) {
			sb.append(v).append(',');
		}
		sb.append(t.dt()).append(',').append(t.terminated() ? 1 : 0);
		return sb.toString();
	}
}
