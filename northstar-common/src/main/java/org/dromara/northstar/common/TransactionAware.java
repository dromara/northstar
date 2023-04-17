package org.dromara.northstar.common;

import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

public interface TransactionAware {
	
	
	void onOrder(OrderField order);

	
	void onTrade(TradeField trade);
}
