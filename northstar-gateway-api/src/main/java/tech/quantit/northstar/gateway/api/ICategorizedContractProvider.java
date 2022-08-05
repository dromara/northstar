package tech.quantit.northstar.gateway.api;

import java.util.List;

import tech.quantit.northstar.common.GatewayType;
import tech.quantit.northstar.common.model.ContractDefinition;
import xyz.redtorch.pb.CoreField.ContractField;

public interface ICategorizedContractProvider {
	
	GatewayType gatewayType();

	String nameOfCategory();
	
	List<ContractDefinition> loadContractDefinitions();
	
	List<ContractField> loadContracts();
	
}
