package tech.quantit.northstar.strategy.api;

import xyz.redtorch.pb.CoreField.ContractField;

public interface ContractBindedAware {

	String bindedContractSymbol();
	
	void setBindedContract(ContractField contract);
}
