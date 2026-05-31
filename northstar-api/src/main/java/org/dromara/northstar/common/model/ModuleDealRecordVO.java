package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleDealRecordVO {
	
	private String name;

	private String symbol;
	
	private String contractId;
	
	private PositionDirectionEnum dir;
	
	private double vol;
	
	private String openPrice;
	
	private String closePrice;
	
	private int profit;
	
	private int transactionFee;
	
	private long closeTimestamp;
	
	private long openTimestamp;
}
