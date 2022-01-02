package common;

import org.apache.commons.lang3.RandomStringUtils;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.model.CtpSettings;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.GatewaySettings;
import tech.quantit.northstar.common.model.SimSettings;

public class TestGatewayFactory {

	public static GatewaySettings makeGatewaySettings(Class<? extends GatewaySettings> type) {
		if(type == CtpSettings.class) {
			return CtpSettings.builder()
					.brokerId("9999")
					.userId(RandomStringUtils.random(10))
					.password(RandomStringUtils.random(10))
					.build();
		}
		if(type == SimSettings.class) {
			return SimSettings.builder()
					.fee(1)
					.build();
		}
		return null;
	}
	
	public static GatewayDescription makeMktGateway(String id, GatewayType type, Object settings, boolean autoConnect) {
		return GatewayDescription.builder()
				.gatewayId(id)
				.autoConnect(autoConnect)
				.gatewayType(type)
				.gatewayAdapterType(RandomStringUtils.random(100))
				.settings(settings)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
	}
	
	public static GatewayDescription makeTrdGateway(String id, String bindGateway, GatewayType type, Object settings, boolean autoConnect) {
		return GatewayDescription.builder()
				.gatewayId(id)
				.autoConnect(autoConnect)
				.gatewayType(type)
				.gatewayAdapterType(RandomStringUtils.random(100))
				.settings(settings)
				.gatewayUsage(GatewayUsage.TRADE)
				.bindedMktGatewayId(bindGateway)
				.build();
	}
}
