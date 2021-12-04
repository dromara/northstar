package tech.xuanwu.northstar.strategy.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleTradeRecord {

	private String moduleName;

	private String contractName;
	
	private String operation;
	
	private String tradingDay;
	
	private long actionTime;
	
	private int volume;
	
	private double price;
	
}
