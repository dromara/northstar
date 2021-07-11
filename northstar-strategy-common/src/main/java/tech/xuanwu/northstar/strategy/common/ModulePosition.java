package tech.xuanwu.northstar.strategy.common;

import java.time.Duration;
import java.util.List;

import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 用于记录模组持仓状态,以计算持仓盈亏,持仓状态,持仓时间
 * @author KevinHuangwl
 *
 */
public interface ModulePosition {

	/**
	 * 获取开仓成交（当策略模型为套利时，或者策略具有加仓逻辑时，开仓成交有多个）
	 * @return
	 */
	List<TradeField> getOpenningTrade();
	
	/**
	 * 获取持仓时长
	 * @return
	 */
	Duration getPositionDuration();
	
	/**
	 * 获取仓位手数
	 * @return
	 */
	int getPositionProfit();
	
	/**
	 * 更新行情
	 * @param tick
	 */
	void onTick(TickField tick);
	
	/**
	 * 更新成交
	 * @param order
	 */
	void onTrade(TradeField trade);
	
	/**
	 * 获取平仓指令
	 * @return
	 */
	OffsetFlagEnum getClosingOffsetFlag(String tradingDay);
}
