package xyz.redtorch.gateway.ctp.x64v6v5v1cpv;

import org.dromara.northstar.common.IDataServiceManager;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.ctp.CtpGatewaySettings;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;

import xyz.redtorch.pb.CoreField.GatewaySettingField.CtpApiSettingField;

public class CtpSimGatewayFactory implements GatewayFactory{

	private FastEventEngine fastEventEngine;
	private IMarketCenter mktCenter;
	private IDataServiceManager dataMgr;

	public CtpSimGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter, IDataServiceManager dataMgr) {
		this.fastEventEngine = fastEventEngine;
		this.mktCenter = mktCenter;
		this.dataMgr = dataMgr;
	}

	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		CtpGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), CtpGatewaySettings.class);
		JSONObject json = dataMgr.getCtpMetaSettings(settings.getBrokerId());
		CtpApiSettingField ctpSetting = CtpApiSettingField.newBuilder()
				.setPassword(settings.getPassword())
				.setUserId(settings.getUserId())
				.setBrokerId(settings.getBrokerId())
				.setAppId(json.getString("appId"))
				.setUserProductInfo(json.getString("appId"))
				.setMdPort(json.getString("mdPort"))
				.setTdPort(json.getString("tdPort"))
				.setAuthCode(json.getString("authCode"))
				.build();
		return new CtpSimGatewayAdapter(fastEventEngine, gatewayDescription.toBuilder().settings(ctpSetting).build(), mktCenter);
	}

}
