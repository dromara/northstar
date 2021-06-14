package tech.xuanwu.northstar.strategy.common;

import java.util.List;

import tech.xuanwu.northstar.strategy.common.model.DealRecord;
import xyz.redtorch.pb.CoreField.TradeField;

public interface ModuleTrade {

	/**
	 * 获取交易记录
	 * @return
	 */
	List<DealRecord> getDealRecords();
	/**
	 * 获取交易总盈亏
	 * @return
	 */
	int getTotalProfit();
	/**
	 * 更新交易记录
	 * @param trade
	 */
	void updateTrade(TradeField trade);
}
