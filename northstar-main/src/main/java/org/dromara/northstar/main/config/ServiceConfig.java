package org.dromara.northstar.main.config;

import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.domain.account.TradeDayAccount;
import org.dromara.northstar.domain.gateway.GatewayAndConnectionManager;
import org.dromara.northstar.main.ExternalJarClassLoader;
import org.dromara.northstar.main.handler.internal.ModuleManager;
import org.dromara.northstar.main.service.AccountService;
import org.dromara.northstar.main.service.GatewayService;
import org.dromara.northstar.main.service.LogService;
import org.dromara.northstar.main.service.ModuleService;
import org.dromara.northstar.main.utils.ModuleFactory;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.gateway.api.GatewayMetaProvider;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import tech.quantit.northstar.gateway.api.utils.MarketDataRepoFactory;

@DependsOn({ 
	"internalDispatcher", 
	"broadcastEventDispatcher", 
	"strategyDispatcher", 
	"accountEventHandler",
	"connectionEventHandler", 
	"moduleFactory" })
@Configuration
public class ServiceConfig {

    @Bean
    AccountService accountService(ConcurrentMap<String, TradeDayAccount> accountMap) {
        return new AccountService(accountMap);
    }

    @Bean
    GatewayService gatewayService(GatewayAndConnectionManager gatewayConnMgr, IGatewayRepository gatewayRepo,
                                                IPlaybackRuntimeRepository playbackRtRepo, IModuleRepository moduleRepo, ISimAccountRepository simAccRepo, GatewayMetaProvider metaProvider,
                                                GatewayMetaProvider settingsPvd, IMarketCenter mktCenter) {
        return new GatewayService(gatewayConnMgr, settingsPvd, metaProvider, mktCenter, gatewayRepo, simAccRepo, playbackRtRepo, moduleRepo);
    }

    @Bean
    ModuleService moduleService(ApplicationContext ctx, ExternalJarClassLoader extJarLoader, IModuleRepository moduleRepo,
                                              MarketDataRepoFactory mdRepoFactory, ModuleFactory moduleFactory, ModuleManager moduleMgr,
                                              IContractManager contractMgr) {
        return new ModuleService(ctx, extJarLoader, moduleRepo, mdRepoFactory, moduleFactory, moduleMgr, contractMgr);
    }

    @Bean
    LogService logService(LoggingSystem loggingSystem) {
        return new LogService(loggingSystem);
    }
	
}
