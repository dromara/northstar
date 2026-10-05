package org.dromara.northstar.rl.training;

import java.nio.file.Path;

/**
 * 单轮离线训练执行器：唯一的跨语言边界。
 *
 * 生产实现为 {@link PythonTrainExecutor}（子进程无状态，崩溃不拖垮 JVM）；
 * 测试可替换为不落盘 PyTorch 模型的桩实现。
 */
public interface RLTrainExecutor {

	/**
	 * 执行一轮离线训练：读 checkpoint + 经验数据 -> 训练 -> 存 checkpoint -> 导出 model.pt。
	 *
	 * @param modelDir		模型目录（含模型级 metadata.json）
	 * @param version		训练版本号（版本目录 versions/v{version}）
	 * @param gradSteps		本轮梯度更新步数
	 * @param lrOverride	覆盖版本 metadata 中的学习率（轮边界调参；null 表示沿用）
	 * @param seed			随机种子
	 * @return				本轮训练结果
	 */
	RLTrainResult trainRound(Path modelDir, int version, int gradSteps, Double lrOverride, long seed) throws Exception;

	/**
	 * @param loss			本轮平均 loss（Python stdout 结构化输出解析而来）
	 * @param trainedSteps	实际执行的梯度步数
	 * @param bufferSize	训练后经验池大小
	 */
	record RLTrainResult(double loss, int trainedSteps, int bufferSize) {
	}
}
