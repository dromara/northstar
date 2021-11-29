package tech.xuanwu.northstar.strategy.api.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.xuanwu.northstar.strategy.api.constant.ModuleState;

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
	
	/**
	 * 多头仓位
	 */
	private Map<String, ModulePositionInfo> longPositions;
	
	/**
	 * 空头仓位
	 */
	private Map<String, ModulePositionInfo> shortPositions;
}
