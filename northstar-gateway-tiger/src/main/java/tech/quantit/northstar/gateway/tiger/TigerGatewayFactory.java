package tech.quantit.northstar.gateway.tiger;

import com.alibaba.fastjson.JSON;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.IMarketCenter;

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
		return new TigerGatewayAdapter(gatewayDescription, feEngine);
	}

}
