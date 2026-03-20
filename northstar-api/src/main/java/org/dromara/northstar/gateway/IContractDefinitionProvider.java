package org.dromara.northstar.gateway;

import java.util.List;

import org.dromara.northstar.common.model.core.ContractDefinition;

public interface IContractDefinitionProvider {

	List<ContractDefinition> get();
	
}
