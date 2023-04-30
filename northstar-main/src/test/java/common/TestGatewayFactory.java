package common;

import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.GatewaySettings;
import org.dromara.northstar.gateway.ctp.CtpGatewaySettings;

public class TestGatewayFactory {

	public static GatewaySettings makeGatewaySettings(Class<? extends GatewaySettings> type) {
		if(type == CtpGatewaySettings.class) {
			CtpGatewaySettings settings = new CtpGatewaySettings();
			settings.setBrokerId("1080");
			settings.setUserId("kevin");
			settings.setPassword("123456");
			return settings;
		}
		return null;
	}
	
	public static GatewayDescription makeMktGateway(String id, ChannelType type, Object settings, boolean autoConnect) {
		return GatewayDescription.builder()
				.gatewayId(id)
				.autoConnect(autoConnect)
				.channelType(type)
				.settings(settings)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.subscribedContracts(List.of())
				.build();
	}
	
	public static GatewayDescription makeTrdGateway(String id, String bindGateway, ChannelType type, Object settings, boolean autoConnect) {
		return GatewayDescription.builder()
				.gatewayId(id)
				.autoConnect(autoConnect)
				.channelType(type)
				.settings(settings)
				.gatewayUsage(GatewayUsage.TRADE)
				.bindedMktGatewayId(bindGateway)
				.build();
	}
}
