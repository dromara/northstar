package xyz.redtorch.gateway.ctp.x64v6v3v15v;

import com.alibaba.fastjson.JSON;

import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.api.AbstractGatewayFactory;
import tech.xuanwu.northstar.gateway.api.Gateway;
import xyz.redtorch.pb.CoreEnum.GatewayAdapterTypeEnum;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.GatewaySettingField.CtpApiSettingField;

public class CtpGatewayFactory extends AbstractGatewayFactory{

	private FastEventEngine fastEventEngine;
	
	public CtpGatewayFactory(FastEventEngine fastEventEngine) {
		this.fastEventEngine = fastEventEngine;
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		GatewayTypeEnum gwType = gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA
				? GatewayTypeEnum.GTE_MarketData
				: GatewayTypeEnum.GTE_Trade;
		CtpSettings settings = JSON.toJavaObject((JSON)JSON.toJSON(gatewayDescription.getSettings()), CtpSettings.class);
		CtpApiSettingField ctpSetting = CtpApiSettingField.newBuilder()
				.setAppId(settings.getAppId())
				.setAuthCode(settings.getAuthCode())
				.setBrokerId(settings.getBrokerId())
				.setMdHost(settings.getMdHost())
				.setMdPort(settings.getMdPort())
				.setTdHost(settings.getTdHost())
				.setTdPort(settings.getTdPort())
				.setPassword(settings.getPassword())
				.setUserId(settings.getUserId())
				.setUserProductInfo(settings.getUserProductInfo())
				.build();
		return new CtpGatewayAdapter(fastEventEngine, GatewaySettingField.newBuilder()
				.setGatewayAdapterType(GatewayAdapterTypeEnum.GAT_CTP)
				.setGatewayId(gatewayDescription.getGatewayId())
				.setGatewayName(gatewayDescription.getGatewayId())
				.setCtpApiSetting(ctpSetting)
				.setGatewayType(gwType)
				.build());
	}

}
