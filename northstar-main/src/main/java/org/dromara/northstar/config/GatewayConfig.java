package org.dromara.northstar.config;

import java.util.EnumMap;
import java.util.Map;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewaySettings;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.playback.PlaybackGatewayFactory;
import org.dromara.northstar.gateway.playback.PlaybackGatewaySettings;
import org.dromara.northstar.gateway.sim.trade.SimGatewayFactory;
import org.dromara.northstar.gateway.utils.MarketDataRepoFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class GatewayConfig {

    @Bean
    GatewayMetaProvider gatewayMetaProvider(FastEventEngine fastEventEngine, IMarketCenter mktCenter, MarketDataRepoFactory mdRepoFactory, 
    		IPlaybackRuntimeRepository rtRepo, ISimAccountRepository accRepo) {
        Map<ChannelType, GatewaySettings> settingsMap = new EnumMap<>(ChannelType.class);
        settingsMap.put(ChannelType.PLAYBACK, new PlaybackGatewaySettings());

        Map<ChannelType, GatewayFactory> factoryMap = new EnumMap<>(ChannelType.class);
        factoryMap.put(ChannelType.PLAYBACK, new PlaybackGatewayFactory(fastEventEngine, mktCenter, rtRepo, mdRepoFactory));
        factoryMap.put(ChannelType.SIM, new SimGatewayFactory(fastEventEngine, accRepo, mktCenter));
        return new GatewayMetaProvider(settingsMap, factoryMap);
    }
}
