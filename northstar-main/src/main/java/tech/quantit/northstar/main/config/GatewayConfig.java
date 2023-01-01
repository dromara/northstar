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
import tech.quantit.northstar.gateway.api.GatewayMetaProvider;
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
	public GatewayFactory tigerGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter) {
		return new TigerGatewayFactory();
	}
	
	@Bean 
	public GatewayMetaProvider gatewayMetaProvider(FastEventEngine fastEventEngine, IMarketCenter mktCenter, IDataServiceManager dataMgr,
			IHolidayManager holidayMgr, IMarketDataRepository mdRepo, IPlaybackRuntimeRepository rtRepo,
			ISimAccountRepository accRepo, SimMarket simMarket) {
		Map<ChannelType, GatewaySettings> settingsMap = new EnumMap<>(ChannelType.class);
		settingsMap.put(ChannelType.CTP, new CtpGatewaySettings());
		settingsMap.put(ChannelType.CTP_SIM, new CtpSimGatewaySettings());
		settingsMap.put(ChannelType.PLAYBACK, new PlaybackGatewaySettings());
		
		Map<ChannelType, GatewayFactory> factoryMap = new EnumMap<>(ChannelType.class);
		factoryMap.put(ChannelType.CTP, new CtpGatewayFactory(fastEventEngine, mktCenter, dataMgr));
		factoryMap.put(ChannelType.CTP_SIM, new CtpSimGatewayFactory(fastEventEngine, mktCenter, dataMgr));
		factoryMap.put(ChannelType.PLAYBACK, new PlaybackGatewayFactory(fastEventEngine, mktCenter, holidayMgr, rtRepo, mdRepo));
		factoryMap.put(ChannelType.SIM, new SimGatewayFactory(fastEventEngine, simMarket, accRepo, mktCenter));
		return new GatewayMetaProvider(settingsMap, factoryMap);
	}
}
