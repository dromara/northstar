package tech.quantit.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模组持仓信息
 * @author KevinHuangwl
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModulePositionDescription {

	/**
	 * 逻辑持仓
	 * 0代表无持仓，正数代表多头n手，负数代表空头n手
	 */
	private int logicalPosition;
	
	/**
	 * 逻辑持仓盈亏
	 */
	private double logicalPositionProfit;
	
	/**
	 * 未平仓成交（以TradeField的字节数组表示一个未平仓成交）
	 * 也可以理解为物理持仓
	 */
	private List<byte[]> uncloseTrades;
}
