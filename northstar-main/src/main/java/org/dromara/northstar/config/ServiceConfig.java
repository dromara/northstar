package org.dromara.northstar.config;

import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.domain.account.TradeDayAccount;
import org.dromara.northstar.domain.gateway.GatewayAndConnectionManager;
import org.dromara.northstar.event.ModuleManager;
import org.dromara.northstar.gateway.api.GatewayMetaProvider;
import org.dromara.northstar.gateway.api.IContractManager;
import org.dromara.northstar.gateway.api.IMarketCenter;
import org.dromara.northstar.gateway.api.utils.MarketDataRepoFactory;
import org.dromara.northstar.main.ExternalJarClassLoader;
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

@DependsOn({ 
	"internalDispatcher", 
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
