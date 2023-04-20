package org.dromara.northstar.config;

import java.util.EnumMap;
import java.util.Map;

import org.dromara.northstar.common.IDataServiceManager;
import org.dromara.northstar.common.IHolidayManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewaySettings;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.common.GatewayFactory;
import org.dromara.northstar.gateway.common.GatewayMetaProvider;
import org.dromara.northstar.gateway.common.IMarketCenter;
import org.dromara.northstar.gateway.common.utils.MarketDataRepoFactory;
import org.dromara.northstar.gateway.ctp.CtpGatewaySettings;
import org.dromara.northstar.gateway.ctp.CtpSimGatewaySettings;
import org.dromara.northstar.gateway.okx.OkxGatewayFactory;
import org.dromara.northstar.gateway.okx.OkxGatewaySettings;
import org.dromara.northstar.gateway.playback.PlaybackGatewayFactory;
import org.dromara.northstar.gateway.playback.PlaybackGatewaySettings;
import org.dromara.northstar.gateway.sim.trade.SimGatewayFactory;
import org.dromara.northstar.gateway.sim.trade.SimMarket;
import org.dromara.northstar.gateway.tiger.TigerGatewayFactory;
import org.dromara.northstar.gateway.tiger.TigerGatewaySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;

@Configuration
class GatewayConfig {

    @Bean
    SimMarket simMarket() {
        return new SimMarket();
    }

    @Bean
    GatewayMetaProvider gatewayMetaProvider(FastEventEngine fastEventEngine, IMarketCenter mktCenter, IDataServiceManager dataMgr,
                                                          IHolidayManager holidayMgr, MarketDataRepoFactory mdRepoFactory, IPlaybackRuntimeRepository rtRepo,
                                                          ISimAccountRepository accRepo, SimMarket simMarket) {
        Map<ChannelType, GatewaySettings> settingsMap = new EnumMap<>(ChannelType.class);
        settingsMap.put(ChannelType.CTP, new CtpGatewaySettings());
        settingsMap.put(ChannelType.CTP_SIM, new CtpSimGatewaySettings());
        settingsMap.put(ChannelType.PLAYBACK, new PlaybackGatewaySettings());
        settingsMap.put(ChannelType.TIGER, new TigerGatewaySettings());
        settingsMap.put(ChannelType.OKX, new OkxGatewaySettings());

        Map<ChannelType, GatewayFactory> factoryMap = new EnumMap<>(ChannelType.class);
        factoryMap.put(ChannelType.CTP, new CtpGatewayFactory(fastEventEngine, mktCenter, dataMgr));
        factoryMap.put(ChannelType.CTP_SIM, new CtpSimGatewayFactory(fastEventEngine, mktCenter, dataMgr));
        factoryMap.put(ChannelType.PLAYBACK, new PlaybackGatewayFactory(fastEventEngine, mktCenter, holidayMgr, rtRepo, mdRepoFactory));
        factoryMap.put(ChannelType.SIM, new SimGatewayFactory(fastEventEngine, simMarket, accRepo, mktCenter));
        factoryMap.put(ChannelType.TIGER, new TigerGatewayFactory(fastEventEngine, mktCenter));
        factoryMap.put(ChannelType.OKX, new OkxGatewayFactory(fastEventEngine, mktCenter));
        return new GatewayMetaProvider(settingsMap, factoryMap);
    }
}
