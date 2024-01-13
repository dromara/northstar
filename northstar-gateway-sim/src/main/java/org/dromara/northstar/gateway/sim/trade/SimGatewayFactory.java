package org.dromara.northstar.gateway.sim.trade;

import java.util.Map;

import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.SimAccountDescription;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.sim.market.SimMarketGatewayLocal;
import org.dromara.northstar.gateway.sim.market.SimTickGenerator;

public class SimGatewayFactory implements GatewayFactory{
	
	private FastEventEngine fastEventEngine;
	
	private ISimAccountRepository simAccountRepo;
	
	private IMarketCenter mktCenter;
	
	private Map<String, SimTickGenerator> tickGenMap;
	
	public SimGatewayFactory(FastEventEngine fastEventEngine, ISimAccountRepository repo, IMarketCenter mktCenter, Map<String, SimTickGenerator> tickGenMap) {
		this.fastEventEngine = fastEventEngine;
		this.simAccountRepo = repo;
		this.mktCenter = mktCenter;
		this.tickGenMap = tickGenMap;
	}

	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		if(gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			return getMarketGateway(gatewayDescription);
		}
		return getTradeGateway(gatewayDescription);
	}
	
	private Gateway getMarketGateway(GatewayDescription gatewayDescription) {
		return new SimMarketGatewayLocal(gatewayDescription, fastEventEngine, mktCenter, tickGenMap);
	}
	
	private Gateway getTradeGateway(GatewayDescription gatewayDescription) {
		String accGatewayId = gatewayDescription.getGatewayId();
		SimAccountDescription simAccountDescription = simAccountRepo.findById(accGatewayId);

		final SimGatewayAccount account;
		if(simAccountDescription == null) {
			account = new SimGatewayAccount(accGatewayId);
		} else {
			account = new SimGatewayAccount(simAccountDescription, mktCenter);
		}
		return new SimTradeGatewayLocal(fastEventEngine, gatewayDescription, account, simAccountRepo);
	}

}
