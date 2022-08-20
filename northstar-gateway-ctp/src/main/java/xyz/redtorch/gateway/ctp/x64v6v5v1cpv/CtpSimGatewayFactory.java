package xyz.redtorch.gateway.ctp.x64v6v5v1cpv;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;

import tech.quantit.northstar.CtpGatewaySettings;
import tech.quantit.northstar.common.IDataServiceManager;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import xyz.redtorch.pb.CoreEnum.GatewayAdapterTypeEnum;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.GatewaySettingField.CtpApiSettingField;

public class CtpSimGatewayFactory implements GatewayFactory{

	private FastEventEngine fastEventEngine;
	private GlobalMarketRegistry registry;
	private IDataServiceManager dataMgr;

	public CtpSimGatewayFactory(FastEventEngine fastEventEngine, GlobalMarketRegistry registry, IDataServiceManager dataMgr) {
		this.fastEventEngine = fastEventEngine;
		this.registry = registry;
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
		return new CtpSimGatewayAdapter(fastEventEngine, GatewaySettingField.newBuilder()
				.setGatewayAdapterType(GatewayAdapterTypeEnum.GAT_CTP)
				.setGatewayId(gatewayDescription.getGatewayId())
				.setGatewayName(gatewayDescription.getGatewayId())
				.setCtpApiSetting(ctpSetting)
				.setGatewayType(gwType)
				.build(), registry);
	}

}
