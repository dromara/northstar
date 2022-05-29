package tech.quantit.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模拟账户信息描述
 * @author KevinHuangwl
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SimAccountDescription {
	/**
	 * 模拟账户ID
	 */
	private String gatewayId;
	/**
	 * 累计平仓盈亏
	 */
	private double totalCloseProfit;
	/**
	 * 累计手续费
	 */
	private double totalCommission;
	/**
	 * 累计入金
	 */
	private double totalDeposit;
	/**
	 * 累计出金
	 */
	private double totalWithdraw;
	/**
	 * 未平仓（开仓）成交
	 */
	private List<TradeField> openTrades;
}
