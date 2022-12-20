package tech.quantit.northstar.gateway.ctp;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.GatewayType;
import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.model.ContractDefinition;
import tech.quantit.northstar.gateway.api.ICategorizedContractProvider;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

@Component
public class CtpFutureContractProvider implements ICategorizedContractProvider {

	@Autowired
	IContractManager contractMgr;
	
	@Autowired
	CTP ctp;
	
	@Override
	public GatewayType gatewayType() {
		return ctp;
	}
	
	@Override
	public String nameOfCategory() {
		return "CTP期货";
	}

	@Override
	public List<ContractDefinition> loadContractDefinitions() {
		return contractMgr.getAllContractDefinitions()
				.stream()
				.filter(item -> item.getGatewayType().equals(ctp.name()) && item.getProductClass() == ProductClassEnum.FUTURES)
				.toList();
	}

	@Override
	public List<ContractField> loadContracts() {
		return loadContractDefinitions()
				.stream()
				.map(def -> contractMgr.relativeContracts(def.contractDefId()))
				.flatMap(Collection::stream)
				.toList();
	}

}
