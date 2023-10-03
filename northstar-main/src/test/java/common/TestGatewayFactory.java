package common;

import java.util.List;

import org.dromara.northstar.common.GatewaySettings;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.constant.PlaybackPrecision;
import org.dromara.northstar.common.constant.PlaybackSpeed;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.playback.PlaybackGatewaySettings;

public class TestGatewayFactory {

	public static GatewaySettings makeGatewaySettings(Class<? extends GatewaySettings> type) {
		if(type == PlaybackGatewaySettings.class) {
			PlaybackGatewaySettings settings = new PlaybackGatewaySettings();
			settings.setPreStartDate("20230401");
			settings.setStartDate("20230501");
			settings.setEndDate("20230506");
			settings.setPrecision(PlaybackPrecision.LOW);
			settings.setSpeed(PlaybackSpeed.RUSH);
			settings.setPlayContracts(List.of(ContractSimpleInfo.builder().channelType(ChannelType.PLAYBACK).unifiedSymbol("rb0000@SHFE@FUTURES").value("rb0000@SHFE@FUTURES@PLAYBACK").build()));
			return settings;
		}
		return new GatewaySettings() {};
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
