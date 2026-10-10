package org.dromara.northstar.rl.agent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.dromara.northstar.rl.model.RLModelMetadata;
import org.dromara.northstar.strategy.rl.RLStatePreprocessor;
import org.dromara.northstar.strategy.rl.TradeAction;

import ai.djl.MalformedModelException;

/**
 * 可刷新的 RL 推理 Agent：绑定模型版本目录，内部持有一份 {@link DqnAgent} 只读副本。
 *
 * DJL Predictor 非线程安全，因此每个交易环境（TradingEnvironment）各自持有一个 RLAgent 实例；
 * 它们是同一个 model.pt 的多个只读副本，每轮训练完成后必须同步 {@link #reload()} 刷新全部实例。
 *
 * 刷新规避文件锁：调用方须先 {@link #close()} 或 {@link #markStale()} 关闭旧句柄，
 * 待 Python 训练侧完成原子切换（先写临时文件再替换）后再 reload。
 * 模型文件不存在时（首轮冷启动）agent 处于未就绪状态，由环境退化为纯随机策略。
 */
public class RLAgent implements AutoCloseable {

	private final Path modelDir;
	private final int version;
	/** 策略提供的 custom 预处理器（仅 metadata 声明 custom 时必需）；null = 由 metadata 决定 identity/standardize */
	private final RLStatePreprocessor customPreprocessor;

	private DqnAgent<TradeAction> delegate;	// null = 未就绪（模型文件不存在）
	private int stateDim;

	public RLAgent(Path modelDir, int version) {
		this(modelDir, version, null);
	}

	public RLAgent(Path modelDir, int version, RLStatePreprocessor customPreprocessor) {
		this.modelDir = modelDir;
		this.version = version;
		this.customPreprocessor = customPreprocessor;
		reload();
	}

	public int stateDim() {
		return stateDim;
	}

	/** 是否已加载模型（false = 冷启动，调用方应使用纯随机策略） */
	public boolean isReady() {
		return delegate != null;
	}

	/** 贪心决策：Q 值 argmax。模型未就绪时抛异常。 */
	public TradeAction act(float[] state) {
		if (delegate == null) {
			throw new IllegalStateException("模型未就绪（首轮冷启动应使用随机策略）");
		}
		try {
			return delegate.act(state);
		} catch (Exception e) {
			throw new IllegalStateException("模型推理失败", e);
		}
	}

	/** 关闭旧句柄并重新加载版本目录（每轮训练完成后调用） */
	public final synchronized void reload() {
		closeDelegate();
		Path versionDir = versionDir();
		try {
			RLModelMetadata meta = RLModelMetadata.load(modelDir);
			this.stateDim = meta.getStateDim();
			if (Files.isRegularFile(versionDir.resolve("model.pt"))) {
				delegate = switch (meta.getAlgo()) {
					case DQN -> DqnAgent.fromVersionDir(versionDir, TradeAction.class, customPreprocessor);
					case PPO -> throw new UnsupportedOperationException("PPO 推理侧将在 v2 提供");
				};
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (MalformedModelException e) {
			throw new IllegalStateException("模型文件加载失败: " + versionDir, e);
		}
	}

	/** 仅关闭旧句柄（训练前调用，规避 Windows 文件锁） */
	public synchronized void markStale() {
		closeDelegate();
	}

	@Override
	public synchronized void close() {
		closeDelegate();
	}

	private void closeDelegate() {
		if (delegate != null) {
			delegate.close();
			delegate = null;
		}
	}

	private Path versionDir() {
		return modelDir.resolve("versions").resolve("v" + version);
	}
}
