package xyz.redtorch.gateway.ctp.x64v6v3v15v;

import com.alibaba.fastjson.JSON;

import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.CtpSettings;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.AbstractGatewayFactory;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import xyz.redtorch.pb.CoreEnum.GatewayAdapterTypeEnum;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.GatewaySettingField.CtpApiSettingField;

public class CtpGatewayFactory extends AbstractGatewayFactory{

	private FastEventEngine fastEventEngine;
	
	private GlobalMarketRegistry registry;
	
	public CtpGatewayFactory(FastEventEngine fastEventEngine, GlobalMarketRegistry registry) {
		this.fastEventEngine = fastEventEngine;
		this.registry = registry;
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
				.build(), registry);
	}

}
