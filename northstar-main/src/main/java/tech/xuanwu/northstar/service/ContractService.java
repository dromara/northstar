package tech.xuanwu.northstar.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;

import tech.xuanwu.northstar.common.constant.Constants;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.domain.GatewayConnection;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 合约服务
 * @author KevinHuangwl
 *
 */
@Service
public class ContractService {
	
	@Autowired
	@Qualifier(Constants.GATEWAY_CONTRACT_MAP)
	private Map<String, Map<String, ContractField>> gatewayContractMap;

	@Subscribe
	public void onEvent(NorthstarEvent e) {
		if(e.getEvent() == NorthstarEventType.CONNECTING) {
			GatewayConnection conn = (GatewayConnection) e.getData();
			String gatewayId = conn.getGwDescription().getGatewayId();
			gatewayContractMap.put(gatewayId, new ConcurrentHashMap<>());
		} else if(e.getEvent() == NorthstarEventType.CONTRACT) {
			ContractField contract = (ContractField) e.getData();
			String gatewayId = contract.getGatewayId();
			String symbol = contract.getSymbol();
			gatewayContractMap.get(gatewayId).put(symbol, contract);
		}
	}
}
