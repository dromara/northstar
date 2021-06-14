package tech.xuanwu.northstar.strategy.common;

import java.time.Duration;
import java.util.List;

import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

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
	
	
	int getPositionProfit();
	
	/**
	 * 获取持仓数量
	 * @return
	 */
	int getNonfronzenVolume();

	/**
	 * 更新行情
	 * @param tick
	 */
	void onTick(TickField tick);
	
	/**
	 * 更新订单（用于计算冻结仓位）
	 */
	void onOrder(OrderField order);
	
	/**
	 * 更新成交
	 * @param order
	 */
	void onTrade(TradeField order);
}
