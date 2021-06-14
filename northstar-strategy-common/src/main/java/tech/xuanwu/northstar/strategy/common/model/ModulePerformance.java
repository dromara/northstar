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
	 * AccountField byte array
	 */
	private byte[] account;
	
	/**
	 * 模组占用账户比例(单位:%)
	 */
	private double accountShare;
	
	/**
	 * 模组状态
	 */
	private ModuleState moduleState;
	
	private String moduleName;
}
