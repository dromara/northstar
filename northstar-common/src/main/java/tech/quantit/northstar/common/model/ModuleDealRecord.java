package tech.quantit.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

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
	 * 合约中文名称
	 */
	private String contractName;
	/**
	 * 持仓方向
	 */
	private PositionDirectionEnum direction;
	/**
	 * 开仓日期
	 */
	private String openDate;
	/**
	 * 平仓日期
	 */
	private String closeDate;
	/**
	 * 开仓时间
	 */
	private long openTimestamp;
	/**
	 * 平仓时间
	 */
	private long closeTimestamp;
	/**
	 * 手数
	 */
	private int volume;
	/**
	 * 开仓价
	 */
	private double openPrice;
	/**
	 * 平仓价
	 */
	private double closePrice;
	/**
	 * 平仓盈亏
	 */
	private int closeProfit;
	/**
	 * 占用资金估算
	 */
	private double estimatedOccupiedMoney;
}
