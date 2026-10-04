package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BacktestModuleVO {
	
	private String moduleName;

	private ContractSimpleInfo contract;
	
	private Integer totalTrades;
	
	private Integer transactionFees;
	
	private Double balance;
	
	private Double realizedProfit;
	
	private Double unrealizedProfit;
	
	private Double annualizedReturn;
	
	/** 平均盈亏比；null 表示无亏损（全胜或零交易），数学上为 +∞ 不可比 */
	private Double profitLossRatio;
	
	private Double maxDrawdown;
	
	private Double maxDrawdownPercentage;
	
	private Double winRate;
	
	private Integer tradeSize;
	
}
