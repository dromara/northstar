package org.dromara.northstar.config;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IMarketCenter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class GatewayConfig {

    @Bean
    GatewayMetaProvider gatewayMetaProvider(FastEventEngine fastEventEngine, IMarketCenter mktCenter, 
    		IPlaybackRuntimeRepository rtRepo, ISimAccountRepository accRepo) {
        return new GatewayMetaProvider();
    }
}
