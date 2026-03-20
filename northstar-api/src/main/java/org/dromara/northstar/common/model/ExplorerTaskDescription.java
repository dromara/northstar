package org.dromara.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ExplorerTaskDescription {

	/**
	 * 任务名称
	 */
	private String taskName;
	
	/**
	 * 预热起始日
	 */
	private String preloadFrom;
	
	/**
	 * 开始日期
	 */
	private String startDate;
	
	/**
	 * 结束日期
	 */
	private String endDate;
	
	/**
	 * 测试周期
	 */
	private List<Integer> periods; 
	
	/**
	 * 测试合约
	 */
	private List<ContractSimpleInfo> contracts;
	
	/**
	 * 分配金额
	 */
	private int initBalance;
	
	/**
	 * 策略配置信息
	 */
	private StrategyMetaInfo strategySetting;
	
}
