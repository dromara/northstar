package tech.xuanwu.northstar.strategy.common.model;

import lombok.Data;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@Data
public class DealRecord {
	
	private String unifiedSymbol;
	
	private PositionDirectionEnum direction;
	
	private int volume;
	
	private double openPrice;
	
	private double closePrice;
	
	private double closeProfit;
	
	
}
