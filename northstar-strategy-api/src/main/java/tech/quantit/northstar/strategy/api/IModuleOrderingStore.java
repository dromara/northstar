package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.common.TransactionAware;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.gateway.api.TradeGateway;
import xyz.redtorch.pb.CoreField.ContractField;

public interface IModuleOrderingStore extends TransactionAware, ContextAware{

	ModuleState getModuleState();
	
	void registerGatewayBinding(ContractField contract, TradeGateway gateway);
	
}
