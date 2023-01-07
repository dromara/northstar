package tech.quantit.northstar.gateway.binance;

import com.alibaba.fastjson2.JSON;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.IMarketCenter;

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
