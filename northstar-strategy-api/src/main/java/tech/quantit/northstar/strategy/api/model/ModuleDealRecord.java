package tech.quantit.northstar.strategy.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleDealRecord {
	
	private String moduleName;

	private String contractName;
	
	private PositionDirectionEnum direction;
	
	private String tradingDay;
	
	private long openTimestamp;
	
	private long closeTimestamp;
	
	/**
	 * @deprecated 该属性已经废弃，应该采用closeTimestamp属性代替 
	 */
	@Deprecated
	private long dealTimestamp;
	
	private int volume;
	
	private double openPrice;
	
	private double closePrice;
	
	private int closeProfit;
	
	private double estimatedOccupiedMoney;
}
