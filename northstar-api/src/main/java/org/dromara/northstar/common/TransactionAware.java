package org.dromara.northstar.common;

import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Trade;

public interface TransactionAware {
	
	/**
	 * 响应订单事件
	 * @param order
	 */
	void onOrder(Order order);

	/**
	 * 响应成交事件
	 * @param trade
	 */
	void onTrade(Trade trade);
}
