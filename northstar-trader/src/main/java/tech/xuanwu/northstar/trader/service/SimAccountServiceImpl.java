package tech.xuanwu.northstar.trader.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import tech.xuanwu.northstar.exception.NoSuchAccountException;
import tech.xuanwu.northstar.gateway.GatewayApi;
import tech.xuanwu.northstar.service.ISimAccountService;
import tech.xuanwu.northstar.trader.constants.Constants;
import tech.xuanwu.northstar.trader.domain.simulated.SimulatedGateway;

@Service
public class SimAccountServiceImpl implements ISimAccountService{

	@Autowired
	@Qualifier(Constants.TRADABLE_ACCOUNT)
	private Map<String, GatewayApi> gatewayApiMap;

	@Override
	public void deposit(String gatewayId, int money) {
		GatewayApi gateway = gatewayApiMap.get(gatewayId);
		if(gateway == null) {
			throw new NoSuchAccountException(gatewayId);
		}
		SimulatedGateway simGateway = (SimulatedGateway) gateway;
		simGateway.getSimMarket().deposit(money);
	}

	@Override
	public void withdraw(String gatewayId, int money) {
		GatewayApi gateway = gatewayApiMap.get(gatewayId);
		if(gateway == null) {
			throw new NoSuchAccountException(gatewayId);
		}
		SimulatedGateway simGateway = (SimulatedGateway) gateway;
		simGateway.getSimMarket().withdraw(money);
	}

}
