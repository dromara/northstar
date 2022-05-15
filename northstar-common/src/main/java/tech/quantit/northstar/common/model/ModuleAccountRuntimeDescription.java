package tech.quantit.northstar.common.model;

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
	 * 账户ID
	 */
	private String accountId;
	
	/**
	 * 初始余额
	 */
	private double initBalance;
	
	/**
	 * 交易手续费
	 */
	private double commissionPerDeal;
	
	/**
	 * 交易手续费
	 */
	private double commissionPerDealInPercentage;

	/**
	 * 期初余额（开仓前计算）
	 */
	private double preBalance;
	
	/**
	 * 累计平仓盈亏
	 */
	private double accCloseProfit;
	
	/**
	 * 累计开平仓手数（开平仓一手算1）
	 */
	private int accDealVolume;
	
	/**
	 * 模组持仓描述
	 */
	private ModulePositionDescription positionDescription;
}
