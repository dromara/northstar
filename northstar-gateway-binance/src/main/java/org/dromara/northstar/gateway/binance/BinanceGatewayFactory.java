package org.dromara.northstar.gateway.binance;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.IMarketCenter;

import com.alibaba.fastjson2.JSON;

public class BinanceGatewayFactory implements GatewayFactory{

	private FastEventEngine fastEventEngine;
	
	private IMarketCenter mktCenter;
	
	public BinanceGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter) {
		this.fastEventEngine = fastEventEngine;
		this.mktCenter = mktCenter;
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		BinanceGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), BinanceGatewaySettings.class);
		gatewayDescription.setSettings(settings);
		new BinanceContractProvider(settings, mktCenter).loadContractOptions();
		return new BinanceMarketGatewayAdapter(gatewayDescription, fastEventEngine);
	}

}
