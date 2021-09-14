package tech.xuanwu.northstar.strategy.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleTradeRecord {

	private String moduleName;

	private String contractName;
	
	private DirectionEnum direction;
	
	private String tradingDay;
	
	private long actionTime;
	
	private int volume;
	
	private double price;
	
}
