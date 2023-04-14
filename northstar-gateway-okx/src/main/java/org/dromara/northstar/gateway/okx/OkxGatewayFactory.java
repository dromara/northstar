package org.dromara.northstar.gateway.okx;

import com.alibaba.fastjson2.JSON;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.IMarketCenter;

public class OkxGatewayFactory implements GatewayFactory{

	private FastEventEngine feEngine;
	
	private IMarketCenter mktCenter;

	private boolean contractLoaded;
	
	public OkxGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter) {
		this.feEngine = fastEventEngine;
		this.mktCenter = mktCenter;
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		OkxGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), OkxGatewaySettings.class);
		gatewayDescription.setSettings(settings);

		if(!contractLoaded) {
			new OkxContractProvider(settings, mktCenter).loadContractOptions();
			contractLoaded = true;
		}

		return switch(gatewayDescription.getGatewayUsage()) {
			case MARKET_DATA -> new OkxMarketGatewayAdapter(gatewayDescription, feEngine, mktCenter);
			case TRADE -> new OkxTradeGatewayAdapter(feEngine, gatewayDescription, mktCenter);
			default -> throw new IllegalArgumentException("未知网关用途：" + gatewayDescription.getGatewayUsage());
		};
	}

}
