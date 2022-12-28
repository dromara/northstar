package xyz.redtorch.gateway.ctp.x64v6v3v15v;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;

import tech.quantit.northstar.common.IDataServiceManager;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import tech.quantit.northstar.gateway.ctp.CtpGatewaySettings;
import xyz.redtorch.pb.CoreEnum.GatewayAdapterTypeEnum;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.GatewaySettingField.CtpApiSettingField;

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
		GatewayTypeEnum gwType = gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA
				? GatewayTypeEnum.GTE_MarketData
				: GatewayTypeEnum.GTE_Trade;
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
		return new CtpGatewayAdapter(fastEventEngine, GatewaySettingField.newBuilder()
				.setGatewayAdapterType(GatewayAdapterTypeEnum.GAT_CTP)
				.setGatewayId(gatewayDescription.getGatewayId())
				.setGatewayName(gatewayDescription.getGatewayId())
				.setCtpApiSetting(ctpSetting)
				.setGatewayType(gwType)
				.build(), mktCenter);
	}

}
