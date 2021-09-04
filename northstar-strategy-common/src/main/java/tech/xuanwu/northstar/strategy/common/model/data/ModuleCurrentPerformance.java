package tech.xuanwu.northstar.strategy.common.model.data;

import java.util.List;
import java.util.Map;

import lombok.Data;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;

@Data
public class ModuleCurrentPerformance {

	/**
	 * BarField byte array list
	 */
	private Map<String, List<byte[]>> refBarDataMap;
	
	/**
	 * 模组持仓总盈亏
	 */
	private double totalPositionProfit;
	
	/**
	 * 模组可用金额
	 */
	private int moduleAvailable;
	
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
