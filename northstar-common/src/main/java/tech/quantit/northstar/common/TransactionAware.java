package tech.quantit.northstar.common;

import com.google.common.eventbus.Subscribe;

import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

public interface TransactionAware extends Subscribable {
	
	@Subscribe
	void onOrder(OrderField order);

	@Subscribe
	void onTrade(TradeField trade);
}
