package org.dromara.northstar.rl.training;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;

import lombok.Getter;
import lombok.Setter;

/**
 * DQN 算法的超参数定义类：字段清单是模型定义的一部分，
 * UI 通过 getMetaInfo() 拿到字段清单后动态渲染表单，前端不硬编码任何超参数项。
 *
 * 超参数取值保存在每个版本的 metadata 中；训练时每轮开始前重读最新取值（UI 改参下一轮生效）。
 */
@Getter
@Setter
public class DqnHyperParams extends DynamicParams {

	@Setting(label = "学习率", type = FieldType.NUMBER, order = 1)
	private double lr = 5e-4;

	@Setting(label = "折扣因子γ（0=按单局长度自动）", type = FieldType.NUMBER, order = 2)
	private double gamma = 0;	// 0 = 自动：时间常数 τ 对齐单局 bar 数（γ=e^(-1/τ)），避免固定 0.99（τ≈100bar）在长持仓转移上过度折扣

	@Setting(label = "起始探索率𝜺", type = FieldType.NUMBER, order = 3)
	private double epsilonStart = 1.0;

	@Setting(label = "最低探索率𝜺", type = FieldType.NUMBER, order = 4)
	private double epsilonEnd = 0.05;

	@Setting(label = "𝜺 衰减轮数", type = FieldType.INTEGER, order = 5)
	private int epsilonDecayRounds = 15;

	@Setting(label = "批次大小", type = FieldType.INTEGER, order = 6)
	private int batchSize = 128;

	@Setting(label = "经验池容量", type = FieldType.INTEGER, order = 7)
	private int bufferCapacity = 100000;

	@Setting(label = "最小训练样本", type = FieldType.INTEGER, order = 8)
	private int minBuffer = 500;

	@Setting(label = "目标网络软更新系数", type = FieldType.NUMBER, order = 9)
	private double tau = 0.005;

	@Setting(label = "最大梯度步数", type = FieldType.INTEGER, order = 10)
	private int maxGradSteps = 2000;

	/** epsilon 衰减轨迹：当前轮数 + 配置起止值计算（轮边界调参即改轨迹） */
	public double epsilonAt(int round) {
		if (epsilonDecayRounds <= 0) {
			return epsilonEnd;
		}
		return Math.max(epsilonEnd, epsilonStart - (epsilonStart - epsilonEnd) * round / epsilonDecayRounds);
	}

	/** 每轮梯度步数：与新增样本量挂钩（约 1:1 回放比），封顶 maxGradSteps */
	public int gradSteps(int newSamples) {
		return Math.min(maxGradSteps, Math.max(300, newSamples));
	}
}
