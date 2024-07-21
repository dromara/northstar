package org.dromara.northstar.gateway.tiger;

import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IMarketCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Order(0)    // 加载顺序需要显式声明，否则会最后才被加载，从而导致加载网关与模组时报异常
@Component
public class TigerLoader implements CommandLineRunner {

    @Autowired
    private IMarketCenter mktCenter;

    @Autowired
    private TigerDataServiceManager dsMgr;

    @Autowired
    private GatewayMetaProvider gatewayMetaProvider;

    @Autowired
    private TigerGatewayFactory tigerFactory;

    public void run(String... args) throws Exception {
        gatewayMetaProvider.add(ChannelType.TIGER, new TigerGatewaySettings(), tigerFactory);

    }

}
