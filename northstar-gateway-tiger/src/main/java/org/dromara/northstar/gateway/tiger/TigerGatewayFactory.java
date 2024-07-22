package org.dromara.northstar.gateway.tiger;

import com.alibaba.fastjson.JSON;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.IMarketCenter;

public class TigerGatewayFactory implements GatewayFactory {

    private FastEventEngine feEngine;

    private IMarketCenter mktCenter;

    private TigerDataServiceManager dataMgr;

    private boolean contractLoaded;

    public TigerGatewayFactory(FastEventEngine feEngine, IMarketCenter mktCenter, TigerDataServiceManager dataMgr) {
        this.feEngine = feEngine;
        this.mktCenter = mktCenter;
        this.dataMgr = dataMgr;
    }

    @Override
    public Gateway newInstance(GatewayDescription gatewayDescription) {
        TigerGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), TigerGatewaySettings.class);
        gatewayDescription.setSettings(settings);
        if (!contractLoaded) {
            new TigerContractProvider(settings, mktCenter, dataMgr).loadContractOptions();
            contractLoaded = true;
        }

        return switch (gatewayDescription.getGatewayUsage()) {
            case MARKET_DATA -> new TigerMarketGatewayAdapter(gatewayDescription, feEngine, mktCenter);
            case TRADE -> new TigerTradeGatewayAdapter(feEngine, gatewayDescription, mktCenter);
            default -> throw new IllegalArgumentException("未知网关用途：" + gatewayDescription.getGatewayUsage());
        };
    }

}
