package tech.xuanwu.northstar.strategy.common.model;

import java.util.List;
import java.util.Map;

import lombok.Data;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;

@Data
public class ModulePerformance {

	/**
	 * BarField byte array list
	 */
	private Map<String, List<byte[]>> refBarDataMap;
	
	/**
	 * 模组交易记录列表
	 */
	private List<DealRecord> dealRecords;
	
	/**
	 * 模组平仓总盈亏
	 */
	private double totalCloseProfit;
	
	/**
	 * 模组持仓总盈亏
	 */
	private double totalPositionProfit;
	
	/**
	 * 模组账户总额
	 */
	private int accountBalance;
	
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
}
