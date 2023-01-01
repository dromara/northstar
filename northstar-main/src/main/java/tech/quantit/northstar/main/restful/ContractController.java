package tech.quantit.northstar.main.restful;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.ContractSimpleInfo;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.gateway.api.IContractManager;

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
				.map(c -> ContractSimpleInfo.builder()
						.name(c.name())
						.unifiedSymbol(c.contractField().getUnifiedSymbol())
						.value(c.identifier().value())
						.build())
				.toList(); 
		return new ResultBean<>(filterAndSort(list, query));
	}
	
	@GetMapping("/subscribed")
	public ResultBean<List<ContractSimpleInfo>> subscribedContracts(String gatewayId, String query){
		GatewayDescription gd0 = gatewayRepo.findById(gatewayId);
		if(Objects.isNull(gd0)) {
			throw new NoSuchElementException("找不到网关：" + gatewayId);
		}
		if(gd0.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			return new ResultBean<>(filterAndSort(gd0.getSubscribedContracts(), query));
		}
		GatewayDescription gd = gatewayRepo.findById(gd0.getBindedMktGatewayId());
		if(Objects.isNull(gd)) {
			throw new NoSuchElementException("找不到网关：" + gatewayId);
		}
		return new ResultBean<>(filterAndSort(gd.getSubscribedContracts(), query));
	}
	
	private List<ContractSimpleInfo> filterAndSort(List<ContractSimpleInfo> list, String query){
		return list.stream()
				.filter(c -> StringUtils.isBlank(query) || c.getName().contains(query) || c.getValue().contains(query))
				.sorted((a, b) -> a.getValue().compareTo(b.getValue()))
				.toList();
	}
}
