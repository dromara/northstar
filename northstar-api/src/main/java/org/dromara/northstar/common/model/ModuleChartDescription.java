package org.dromara.northstar.common.model;

import java.util.List;

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
	
	private List<ContractChartDescription> data;
	
	private List<ListValue> strategyInfos;
}
