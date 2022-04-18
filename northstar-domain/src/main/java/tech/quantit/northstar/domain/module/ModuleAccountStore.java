package tech.quantit.northstar.domain.module;

import java.util.List;

import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

public interface ModuleAccountStore {

	void onOrder(OrderField order);
	
	void onTrade(TradeField trade);
	
	double getInitBalance();
	
	double getPreBalance();
	
	List<TradeField> getUncloseTrade();
	
	int getLogicalPosition();
	
	double getLogicalPositionProfit();
}
