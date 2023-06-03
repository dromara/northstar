package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模组账户信息
 * @author KevinHuangwl
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleAccountRuntimeDescription {
	
	/**
	 * 初始余额
	 */
	private double initBalance;
	/**
	 * 累计平仓盈亏
	 */
	private double accCloseProfit;
	/**
	 * 累计开平仓手数（开平仓一手算1）
	 */
	private int accDealVolume;
	/**
	 * 累计手续费
	 */
	private double accCommission;
	/**
	 * 模组持仓描述
	 */
	@Builder.Default
	private ModulePositionDescription positionDescription = new ModulePositionDescription();
	/**
	 * 最大盈利
	 */
	private double maxProfit;
	/**
	 * 最大回撤
	 */
	private double maxDrawback;
	/**
	 * 平均盈亏
	 */
	private double avgEarning;
	/**
	 * 盈亏标准差
	 */
	private double stdEarning;
}
