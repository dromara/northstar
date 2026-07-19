package org.dromara.northstar.common.model;

import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.constant.Interval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代表多个合约的图表数据
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleChartDescription {
	
	private Map<Interval, List<ContractChartDescription>> data;
	
	private List<ListValue> strategyInfos;
	
}
