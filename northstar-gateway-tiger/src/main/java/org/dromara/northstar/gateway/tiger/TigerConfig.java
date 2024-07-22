package org.dromara.northstar.gateway.tiger;

import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.gateway.IMarketCenter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TigerConfig {

    static {
        log.info("=====================================================");
        log.info("                  加载gateway-tiger                   ");
        log.info("=====================================================");
    }

    @Bean
    TigerDataServiceManager tigerDataServiceManager() {
        return new TigerDataServiceManager();
    }

    @Bean
    TigerGatewayFactory tigerGatewayFactory(FastEventEngine feEngine, IMarketCenter marketCenter,
                                            @Qualifier("tigerDataServiceManager") TigerDataServiceManager dataMgr) {
        return new TigerGatewayFactory(feEngine, marketCenter, dataMgr);
    }

    @Bean
    TigerGatewaySettings tigerGatewaySettings() {
        return new TigerGatewaySettings();
    }
}
