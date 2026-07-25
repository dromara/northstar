package org.dromara.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BacktestModuleTradeDetailsVO {

	private ModuleChartDescription chartData;
	
	private List<ModuleDealRecordVO> dealRecords;
	
}
