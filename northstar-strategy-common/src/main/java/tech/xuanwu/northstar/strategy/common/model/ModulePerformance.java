package tech.xuanwu.northstar.strategy.common.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ModulePerformance {

	/**
	 * BarField byte array list
	 */
	private Map<String, List<byte[]>> refBarDataMap;
	
	
	private List<DealRecord> dealRecords;
	
	
	private double totalProfit;
	
	
	private String moduleName;
}
