package org.dromara.northstar.app.rl;

import java.nio.file.Path;

/**
 * 桩训练执行器：不调用 Python、不产生模型文件，仅返回占位结果。
 *
 * 用于无 PyTorch 环境的集成测试（采样链路全程退化为随机策略冷启动），
 * 通过 northstar.rl.train-executor=stub 启用。生产环境请勿使用。
 */
public class StubTrainExecutor implements org.dromara.northstar.rl.training.RLTrainExecutor {

	@Override
	public RLTrainResult trainRound(Path modelDir, int version, int gradSteps, Double lrOverride, long seed) {
		return new RLTrainResult(Double.NaN, 0, 0);
	}
}
