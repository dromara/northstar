package tech.quantit.northstar.gateway.sim.market;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.GatewayType;
import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.model.ContractDefinition;
import tech.quantit.northstar.gateway.api.ICategorizedContractProvider;
import tech.quantit.northstar.gateway.sim.SIM;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

@Component
public class SimContractProvider implements ICategorizedContractProvider {

	@Autowired
	IContractManager contractMgr;
	
	@Autowired
	SIM sim;
	
	@Override
	public String nameOfCategory() {
		return "期货模拟";
	}

	@Override
	public List<ContractDefinition> loadContractDefinitions() {
		return contractMgr.getAllContractDefinitions()
				.stream()
				.filter(item -> item.getGatewayType().equals("SIM") && item.getProductClass() == ProductClassEnum.FUTURES)
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

	@Override
	public GatewayType gatewayType() {
		return sim;
	}

}
