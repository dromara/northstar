package org.dromara.northstar.common.model.rl;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 训练任务详情：任务信息 + 每轮指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RLTrainingTaskDetailVO {

	private RLTrainingTaskVO task;
	private List<RLRoundMetrics> metrics;
}
