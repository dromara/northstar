package org.dromara.northstar.web.restful;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.contract.IndexContract;
import org.dromara.northstar.gateway.contract.OptionChainContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * 
 * @author KevinHuangwl
 *
 */
@RequestMapping("/northstar/contracts")
@RestController
public class ContractController {

	@Autowired
	IContractManager contractMgr; 
	
	@Autowired
	IGatewayRepository gatewayRepo;
	
	@GetMapping
	public ResultBean<List<ContractSimpleInfo>> channelContracts(ChannelType channelType, String query){
		List<ContractSimpleInfo> list = contractMgr.getContracts(channelType).stream()
				.filter(c -> c.productClass() != ProductClassEnum.OPTION || c.identifier().value().startsWith(Constants.OPTION_CHAIN_PREFIX))
				.map(c -> ContractSimpleInfo.builder()
						.name(c.name())
						.unifiedSymbol(c.contractField().getUnifiedSymbol())
						.channelType(c.channelType())
						.value(c.identifier().value())
						.build())
				.toList(); 
		return new ResultBean<>(filterAndSort(list, query));
	}
	
	@GetMapping("/subscribed")
	public ResultBean<List<ContractSimpleInfo>> subscribedContracts(String gatewayId, String query){
		GatewayDescription gd0 = gatewayRepo.findById(gatewayId);
		if(gd0.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			return new ResultBean<>(filterAndSort(gd0.getSubscribedContracts(), query));
		}
		GatewayDescription gd = gatewayRepo.findById(gd0.getBindedMktGatewayId());
		List<ContractSimpleInfo> subscribedContracts = gd.getSubscribedContracts();
		Set<ContractSimpleInfo> actualSubContracts = new HashSet<>(subscribedContracts);
		subscribedContracts.forEach(csi -> {
			Contract contract = contractMgr.getContract(Identifier.of(csi.getValue()));
			if(contract instanceof IndexContract idxContract) {
				actualSubContracts.addAll(idxContract.memberContracts().stream()
					.map(c -> ContractSimpleInfo.builder()
							.name(c.name())
							.unifiedSymbol(c.contractField().getUnifiedSymbol())
							.channelType(c.channelType())
							.value(c.identifier().value())
							.build())
					.toList());
			}
			if(contract instanceof OptionChainContract optChainContract) {
				actualSubContracts.addAll(optChainContract.memberContracts().stream()
						.map(c -> ContractSimpleInfo.builder()
								.name(c.name())
								.unifiedSymbol(c.contractField().getUnifiedSymbol())
								.value(c.identifier().value())
								.build())
						.toList());
			}
		});
		return new ResultBean<>(filterAndSort(actualSubContracts, query));
	}
	
	private List<ContractSimpleInfo> filterAndSort(Collection<ContractSimpleInfo> list, String query){
		return list.stream()
				.filter(c -> StringUtils.isBlank(query) || c.getName().contains(query) || c.getValue().contains(query))
				.sorted((a, b) -> a.getValue().compareTo(b.getValue()))
				.distinct()
				.toList();
	}
}
