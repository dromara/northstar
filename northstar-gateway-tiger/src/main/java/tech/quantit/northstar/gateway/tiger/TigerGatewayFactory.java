package tech.quantit.northstar.gateway.tiger;

import com.alibaba.fastjson2.JSON;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.IMarketCenter;

public class TigerGatewayFactory implements GatewayFactory{

	private FastEventEngine feEngine;
	
	private IMarketCenter mktCenter;
	
	public TigerGatewayFactory(FastEventEngine feEngine, IMarketCenter mktCenter) {
		this.feEngine = feEngine;
		this.mktCenter = mktCenter;
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		TigerGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), TigerGatewaySettings.class);
		gatewayDescription.setSettings(settings);
		new TigerContractProvider(settings, mktCenter).loadContractOptions();
		return new TigerGatewayAdapter(gatewayDescription, feEngine);
	}

}
