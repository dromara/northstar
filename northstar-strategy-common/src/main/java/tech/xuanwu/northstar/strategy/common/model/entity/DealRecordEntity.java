package tech.xuanwu.northstar.strategy.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DealRecordEntity {
	
	
	private String moduleName;

	private String contractName;
	
	private PositionDirectionEnum direction;
	
	private String tradingDay;
	
	private long dealTimestamp;
	
	private int volume;
	
	private double openPrice;
	
	private double closePrice;
	
	private int closeProfit;
}
