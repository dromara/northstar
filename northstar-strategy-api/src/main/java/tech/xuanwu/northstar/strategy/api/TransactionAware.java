package tech.xuanwu.northstar.strategy.api;

import com.google.common.eventbus.Subscribe;

import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

public interface TransactionAware {
	
	@Subscribe
	void onOrder(OrderField order);

	@Subscribe
	void onTrade(TradeField trade);
}
