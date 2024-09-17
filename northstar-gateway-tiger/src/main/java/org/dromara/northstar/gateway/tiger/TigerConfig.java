package org.dromara.northstar.gateway.tiger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.gateway.*;
import org.dromara.northstar.gateway.tiger.util.CodecUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Configuration
public class TigerConfig {

    static {
        log.info("=====================================================");
        log.info("                  加载gateway-tiger                   ");
        log.info("=====================================================");
    }
    @Autowired
    private IGatewayRepository gatewayRepo;
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
        AtomicReference<TigerGatewaySettings> tigerGatewaySettings = new AtomicReference<>(new TigerGatewaySettings());
        List<GatewayDescription> result = gatewayRepo.findAll();
        result.stream().filter(gd -> gd.getGatewayUsage() == GatewayUsage.MARKET_DATA).map(this::decodeSettings).forEach(gd -> {
            try {
                GatewayDescription gatewayDescription = decodeSettings(gd);
                if (ChannelType.TIGER.equals(gatewayDescription.getChannelType())) {
                    tigerGatewaySettings.set(JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), TigerGatewaySettings.class));
                }
            } catch (Exception e) {
                log.error("", e);
            }
        });

        return tigerGatewaySettings.get();
    }


    private GatewayDescription decodeSettings(GatewayDescription gd) {
        if(gd.getSettings() instanceof JSONObject) {
            return gd;
        }
        String decodeStr = CodecUtils.decrypt((String) gd.getSettings());
        if(!JSON.isValid(decodeStr)) {
            throw new IllegalStateException("解码字符串非法，很可能是临时文件夹" + System.getProperty("user.home") + File.separator
                    + ".northstar-salt这个盐文件与加密时的不一致导致无法解码。解决办法：手动移除旧的Gateway数据，重新录入，并确保盐文件不会丢失。");
        }
        gd.setSettings(JSON.parseObject(decodeStr));
        return gd;
    }
}
