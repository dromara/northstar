package tech.xuanwu.northstar.strategy.common.model.entity;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.model.ModulePosition;

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
	private Map<String, ModulePosition> longPositions;
	
	/**
	 * 空头仓位
	 */
	private Map<String, ModulePosition> shortPositions;
}
