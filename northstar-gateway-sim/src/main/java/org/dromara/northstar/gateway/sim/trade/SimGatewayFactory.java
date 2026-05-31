package org.dromara.northstar.gateway.sim.trade;

import java.util.Map;

import org.dromara.northstar.common.GatewaySettings;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.sim.market.SimMarketGateway;
import org.dromara.northstar.gateway.sim.market.SimTickGenerator;

public class SimGatewayFactory implements GatewayFactory{
	
	private FastEventEngine fastEventEngine;
	
	private IAccountRepository accountRepo;
	
	private MarketCenter mktCenter;
	
	private Map<String, SimTickGenerator> tickGenMap;
	
	public SimGatewayFactory(FastEventEngine fastEventEngine, IAccountRepository accountRepo,
			MarketCenter mktCenter, Map<String, SimTickGenerator> tickGenMap) {
		this.fastEventEngine = fastEventEngine;
		this.accountRepo = accountRepo;
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
		return new SimMarketGateway(gatewayDescription, fastEventEngine, mktCenter, tickGenMap);
	}
	
	private Gateway getTradeGateway(GatewayDescription gatewayDescription) {
		return new SimTradeGateway(fastEventEngine, gatewayDescription, new SimAccountRemote(mktCenter, accountRepo), mktCenter);
	}

	@Override
	public GatewayDescription enhance(GatewayDescription gatewayDescription) {
		// 模拟网关没有配置，不需要增强
		return gatewayDescription;
	}

	@Override
	public GatewaySettings gatewaySettings() {
		return null;
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.SIM;
	}

}
