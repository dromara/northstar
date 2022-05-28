package tech.quantit.northstar.common;

import java.util.List;
import java.util.Set;

import tech.quantit.northstar.common.model.ContractDefinition;
import xyz.redtorch.pb.CoreField.ContractField;

public interface IContractManager {

	public boolean addContract(ContractField contract);
	
	public ContractField getContract(String unifiedSymbol);
	
	public ContractDefinition getContractDefinition(String unifiedSymbol);
	
	public boolean isIndexContract(ContractField contract);
	
	public List<ContractField> monthlyContractsOfIndex(ContractField contract);
	
	public Set<ContractField> relativeContracts(String contractDefId);
	
	public List<ContractDefinition> getAllContractDefinitions();
}
