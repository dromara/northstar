package tech.quantit.northstar.domain.strategy;

import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.gateway.api.TradeGateway;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

public interface ModuleOrderingStore {

	void onOrder(OrderField order);
	
	void onTrade(TradeField trade);
	
	ModuleState getModuleState();
	
	void registerGatewayBinding(ContractField contract, TradeGateway gateway);
}
