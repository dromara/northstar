package common;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.model.CtpSettings;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.GatewaySettings;

public class TestGatewayFactory {

	public static GatewaySettings makeGatewaySettings(Class<? extends GatewaySettings> type) {
		if(type == CtpSettings.class) {
			return CtpSettings.builder()
					.brokerId("9999")
					.userId("kevin")
					.password("123456")
					.build();
		}
		return null;
	}
	
	public static GatewayDescription makeMktGateway(String id, GatewayType type, Object settings, boolean autoConnect) {
		return GatewayDescription.builder()
				.gatewayId(id)
				.autoConnect(autoConnect)
				.gatewayType(type)
				.settings(settings)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
	}
	
	public static GatewayDescription makeTrdGateway(String id, String bindGateway, GatewayType type, Object settings, boolean autoConnect) {
		return GatewayDescription.builder()
				.gatewayId(id)
				.autoConnect(autoConnect)
				.gatewayType(type)
				.settings(settings)
				.gatewayUsage(GatewayUsage.TRADE)
				.bindedMktGatewayId(bindGateway)
				.build();
	}
}
