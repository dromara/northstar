package org.dromara.northstar.ai;

/**
 * 样本数据
 * @auth KevinHuangwl
 */
public record SampleData(
		/**
		 * 合约标识
		 */
		String unifiedSymbol,
		/**
		 * 样本日期
		 */
		String actionDate,
		/**
		 * 样本时间
		 */
		String actionTime,
		/**
		 * 环境状态
		 */
		RLState states,
		/**
		 * 市场价
		 */
		double marketPrice
	) 
{}
