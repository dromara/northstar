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
	
	private String barInterval;
	
	private int totalTrades;
	
	private int transactionFees;
	
	private double balance;
	
	private double realizedProfit;
	
	private double unrealizedProfit;
	
	private double annualizedReturn;
	
	private double profitLossRatio;
	
	private double maxDrawdown;
	
	private double maxDrawdownPercentage;
	
	private double winRate;
	
	private int tradeSize;
	
}
