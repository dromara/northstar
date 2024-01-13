package org.dromara.northstar.common;

import org.dromara.northstar.common.model.core.Contract;

public interface ContractBindedAware {

	String bindedContractSymbol();
	
	void setBindedContract(Contract contract);
}
