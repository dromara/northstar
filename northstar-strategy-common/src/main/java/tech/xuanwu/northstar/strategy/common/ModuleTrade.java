package tech.xuanwu.northstar.strategy.common;

import java.util.List;

import tech.xuanwu.northstar.strategy.common.model.DealRecord;
import tech.xuanwu.northstar.strategy.common.model.TradeDescription;

/**
 * 用于记录模组的所有成交记录,并以此计算得出相应的每次开平仓盈亏,以及开平仓配对
 * @author KevinHuangwl
 *
 */
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
	int getTotalCloseProfit();
	/**
	 * 更新交易记录
	 * @param trade
	 */
	void updateTrade(TradeDescription trade);
}
