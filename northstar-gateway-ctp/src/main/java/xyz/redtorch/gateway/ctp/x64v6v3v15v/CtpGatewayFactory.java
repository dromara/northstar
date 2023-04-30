package xyz.redtorch.gateway.ctp.x64v6v3v15v;

import org.dromara.northstar.common.IDataServiceManager;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.ctp.CtpGatewaySettings;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;

public class CtpGatewayFactory implements GatewayFactory{

	private FastEventEngine fastEventEngine;
	
	private IMarketCenter mktCenter;
	
	private IDataServiceManager dataMgr;
	
	public CtpGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter, IDataServiceManager dataMgr) {
		this.fastEventEngine = fastEventEngine;
		this.mktCenter = mktCenter;
		this.dataMgr = dataMgr;
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		CtpGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), CtpGatewaySettings.class);
		JSONObject json = dataMgr.getCtpMetaSettings(settings.getBrokerId());
		settings.setAppId(json.getString("appId"));
		settings.setAuthCode(json.getString("authCode"));
		settings.setMdPort(json.getString("mdPort"));
		settings.setTdPort(json.getString("tdPort"));
		return new CtpGatewayAdapter(fastEventEngine, gatewayDescription.toBuilder().settings(settings).build(), mktCenter);
	}

}
