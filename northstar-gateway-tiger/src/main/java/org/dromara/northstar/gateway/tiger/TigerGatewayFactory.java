package org.dromara.northstar.gateway.tiger;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.api.Gateway;
import org.dromara.northstar.gateway.api.GatewayFactory;
import org.dromara.northstar.gateway.api.IMarketCenter;

import com.alibaba.fastjson.JSON;

public class TigerGatewayFactory implements GatewayFactory{

	private FastEventEngine feEngine;
	
	private IMarketCenter mktCenter;
	
	private boolean contractLoaded;
	
	public TigerGatewayFactory(FastEventEngine feEngine, IMarketCenter mktCenter) {
		this.feEngine = feEngine;
		this.mktCenter = mktCenter;
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		TigerGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), TigerGatewaySettings.class);
		gatewayDescription.setSettings(settings);
		if(!contractLoaded) {			
			new TigerContractProvider(settings, mktCenter).loadContractOptions();
			contractLoaded = true;
		}
		
		return switch(gatewayDescription.getGatewayUsage()) {
		case MARKET_DATA -> new TigerMarketGatewayAdapter(gatewayDescription, feEngine, mktCenter);
		case TRADE -> new TigerTradeGatewayAdapter(feEngine, gatewayDescription, mktCenter);
		default -> throw new IllegalArgumentException("未知网关用途：" + gatewayDescription.getGatewayUsage());
		};
	}

}
