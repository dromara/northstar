package tech.quantit.northstar.strategy.api.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.ModuleState;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleRealTimeInfo {

	/**
	 * 模组持仓总盈亏
	 */
	private double totalPositionProfit;
	
	/**
	 * 模组平均占用资金
	 */
	private double avgOccupiedAmount;
	
	/**
	 * 模组状态
	 */
	private ModuleState moduleState;
	
	/**
	 * 模组名称
	 */
	private String moduleName;
	
	/**
	 * 账户ID
	 */
	private String accountId;
	
	/**
	 * 多头仓位
	 */
	private Map<String, ModulePositionInfo> longPositions;
	
	/**
	 * 空头仓位
	 */
	private Map<String, ModulePositionInfo> shortPositions;
	
	/**
	 * 最近五次交易的平均盈亏
	 */
	private double meanProfitOf5Transactions;
	
	/**
	 * 最近十次交易的平均盈亏
	 */
	private double meanProfitOf10Transactions;
	
	/**
	 * 最近五次交易的胜率 
	 */
	private double winningRateOf5Transactions;
	
	/**
	 * 最近十次交易的胜率
	 */
	private double winningRateOf10Transactions;
}
