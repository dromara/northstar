package org.dromara.northstar.common;

import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

public interface TransactionAware {
	
	/**
	 * 响应订单事件
	 * @param order
	 */
	void onOrder(OrderField order);

	/**
	 * 响应成交事件
	 * @param trade
	 */
	void onTrade(TradeField trade);
}
