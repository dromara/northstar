package common;

import org.apache.commons.lang3.RandomStringUtils;

import tech.xuanwu.northstar.common.constant.ConnectionState;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.GatewaySettings;
import tech.xuanwu.northstar.common.model.SimSettings;

public class TestGatewayFactory {

	public static GatewaySettings makeGatewaySettings(Class<? extends GatewaySettings> type) {
		if(type == CtpSettings.class) {
			return CtpSettings.builder()
					.appId("testAppId")
					.authCode("testAuthCode")
					.brokerId("9999")
					.mdHost("127.0.0.1")
					.mdPort("12345")
					.tdHost("127.0.0.1")
					.tdPort("65432")
					.userId(RandomStringUtils.random(10))
					.password(RandomStringUtils.random(10))
					.userProductInfo(RandomStringUtils.random(10))
					.build();
		}
		if(type == SimSettings.class) {
			return SimSettings.builder()
					.ticksOfCommission(1)
					.build();
		}
		return null;
	}
	
	public static GatewayDescription makeMktGateway(String id, GatewayType type, Object settings, boolean connected) {
		return GatewayDescription.builder()
				.gatewayId(id)
				.connectionState(connected ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED)
				.gatewayType(type)
				.gatewayAdapterType(RandomStringUtils.random(100))
				.settings(settings)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
	}
	
	public static GatewayDescription makeTrdGateway(String id, String bindGateway, GatewayType type, Object settings, boolean connected) {
		return GatewayDescription.builder()
				.gatewayId(id)
				.connectionState(connected ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED)
				.gatewayType(type)
				.gatewayAdapterType(RandomStringUtils.random(100))
				.settings(settings)
				.gatewayUsage(GatewayUsage.TRADE)
				.bindedMktGatewayId(bindGateway)
				.build();
	}
}
