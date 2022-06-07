package tech.quantit.northstar.common.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
	private List<byte[]> openTrades;
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimAccountDescription other = (SimAccountDescription) obj;
		for(int i=0; i<openTrades.size(); i++) {
			if(!Arrays.equals(openTrades.get(i), other.openTrades.get(i))) {
				return false;
			}
		}
		return Objects.equals(gatewayId, other.gatewayId)
				&& Double.doubleToLongBits(totalCloseProfit) == Double.doubleToLongBits(other.totalCloseProfit)
				&& Double.doubleToLongBits(totalCommission) == Double.doubleToLongBits(other.totalCommission)
				&& Double.doubleToLongBits(totalDeposit) == Double.doubleToLongBits(other.totalDeposit)
				&& Double.doubleToLongBits(totalWithdraw) == Double.doubleToLongBits(other.totalWithdraw);
	}
	@Override
	public int hashCode() {
		return Objects.hash(gatewayId, openTrades, totalCloseProfit, totalCommission, totalDeposit, totalWithdraw);
	}
	
	
}
