package tech.quantit.northstar.main.service;

import java.util.Collection;
import java.util.List;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.model.ContractDefinition;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.gateway.api.ICategorizedContractProvider;
import xyz.redtorch.pb.CoreField.ContractField;

public class ContractService {

	List<ICategorizedContractProvider> contractProviders;
	
	IGatewayRepository gatewayRepo;
	
	IContractManager contractMgr;
	
	public ContractService(List<ICategorizedContractProvider> contractProviders, IGatewayRepository gatewayRepo, IContractManager contractMgr) {
		this.contractProviders = contractProviders;
		this.gatewayRepo = gatewayRepo;
		this.contractMgr = contractMgr;
	}
	
	public List<ContractDefinition> getContractDefinitions(String name){
		return contractProviders.stream()
				.filter(pvd -> pvd.nameOfCategory().equals(name))
				.map(ICategorizedContractProvider::loadContractDefinitions)
				.flatMap(Collection::stream)
				.toList();
	}
	
	public List<ContractField> getContractList(String name){
		return contractProviders.stream()
				.filter(pvd -> pvd.nameOfCategory().equals(name))
				.map(ICategorizedContractProvider::loadContracts)
				.flatMap(Collection::stream)
				.toList();
	}
	
	public List<String> getContractProviders(String gatewayType){
		return contractProviders.stream()
				.filter(pvd -> pvd.gatewayType().name().equals(gatewayType))
				.map(ICategorizedContractProvider::nameOfCategory)
				.toList();
	}
	
	public List<ContractField> getSubscribableContractList(String contractDefId){
		return contractMgr.relativeContracts(contractDefId)
				.stream()
				.toList();
	}
}
