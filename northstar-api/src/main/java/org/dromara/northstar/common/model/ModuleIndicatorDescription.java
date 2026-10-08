package org.dromara.northstar.common.model;

import java.util.List;

import org.dromara.northstar.common.constant.IndicatorPlacement;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.LineStyle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代表了一个模组的指标对象
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleIndicatorDescription {

	/* 指标计算合约 */
	private ContractSimpleInfo contract;
	/* 指标名称 */
	private String name;
	/* 指标周期 */
	private Interval timeFrame;
	/* 指标线样式 */
	@Builder.Default
	private LineStyle style = LineStyle.LINES;
	/* 指标组名（同组指标线在前端聚合成一个指标项，空串表示独立成组） */
	@Builder.Default
	private String group = "";
	/* 指标主副图归属 */
	@Builder.Default
	private IndicatorPlacement placement = IndicatorPlacement.MAIN;
	/* 指标值 */
	private List<TimeSeriesValue> values;
}
