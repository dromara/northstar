package org.dromara.northstar.common;

import xyz.redtorch.pb.CoreField.ContractField;

public interface ContractBindedAware {

	String bindedContractSymbol();
	
	void setBindedContract(ContractField contract);
}
