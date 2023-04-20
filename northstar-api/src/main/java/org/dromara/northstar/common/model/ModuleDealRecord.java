package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交易开平仓记录
 * @author KevinHuangwl
 *
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleDealRecord {
	/**
	 * 模组名称
	 */
	private String moduleName;
	/**
	 * 模组账户
	 */
	private String moduleAccountId;
	/**
	 * 合约中文名称
	 */
	private String contractName;
	/**
	 * 平仓盈亏
	 */
	private double dealProfit;
	/**
	 * 开仓成交
	 */
	private byte[] openTrade;
	/**
	 * 平仓成交
	 */
	private byte[] closeTrade;
}
