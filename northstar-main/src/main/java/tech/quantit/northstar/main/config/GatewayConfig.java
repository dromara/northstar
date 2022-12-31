package tech.quantit.northstar.main.config;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.quantit.northstar.common.IDataServiceManager;
import tech.quantit.northstar.common.IHolidayManager;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewaySettings;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.GatewaySettingsMetaInfoProvider;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import tech.quantit.northstar.gateway.ctp.CtpGatewaySettings;
import tech.quantit.northstar.gateway.ctp.CtpSimGatewaySettings;
import tech.quantit.northstar.gateway.playback.PlaybackGatewayFactory;
import tech.quantit.northstar.gateway.playback.PlaybackGatewaySettings;
import tech.quantit.northstar.gateway.sim.trade.SimGatewayFactory;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.gateway.tiger.TigerGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;

@Configuration
public class GatewayConfig {

	@Bean
	public SimMarket simMarket() {
		return new SimMarket();
	}
	
	@Bean
	public GatewayFactory ctpGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter, IDataServiceManager dataMgr) {
		return new CtpGatewayFactory(fastEventEngine, mktCenter, dataMgr);
	}

	@Bean
	public GatewayFactory ctpSimGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter, IDataServiceManager dataMgr) {
		return new CtpSimGatewayFactory(fastEventEngine, mktCenter, dataMgr);
	}

	@Bean
	public GatewayFactory playbackGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter,
			IHolidayManager holidayMgr, IMarketDataRepository mdRepo, IPlaybackRuntimeRepository rtRepo) {
		return new PlaybackGatewayFactory(fastEventEngine, mktCenter, holidayMgr, rtRepo, mdRepo);
	}

	@Bean
	public GatewayFactory simGatewayFactory(FastEventEngine fastEventEngine, SimMarket simMarket,
			ISimAccountRepository accRepo, IMarketCenter mktCenter) {
		return new SimGatewayFactory(fastEventEngine, simMarket, accRepo, mktCenter);
	}
	
	@Bean
	public GatewayFactory tigerGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter) {
		return new TigerGatewayFactory();
	}
	
	@Bean 
	public GatewaySettingsMetaInfoProvider gatewaySettingsMetaInfoProvider() {
		Map<ChannelType, GatewaySettings> settingsMap = new EnumMap<>(ChannelType.class);
		settingsMap.put(ChannelType.CTP, new CtpGatewaySettings());
		settingsMap.put(ChannelType.CTP_SIM, new CtpSimGatewaySettings());
		settingsMap.put(ChannelType.PLAYBACK, new PlaybackGatewaySettings());
		return new GatewaySettingsMetaInfoProvider(settingsMap);
	}
}
