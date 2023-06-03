package org.dromara.northstar.config;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.support.notification.MailDeliveryManager;
import org.dromara.northstar.web.service.AccountService;
import org.dromara.northstar.web.service.GatewayService;
import org.dromara.northstar.web.service.LogService;
import org.dromara.northstar.web.service.ModuleService;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@DependsOn({ 
	"internalDispatcher", 
	"accountEventHandler",
	"connectionEventHandler", 
})
@Configuration
public class ServiceConfig {

    @Bean
    AccountService accountService(AccountManager accountMgr, IContractManager contractMgr) {
        return new AccountService(accountMgr, contractMgr);
    }

    @Bean
    GatewayService gatewayService() {
        return new GatewayService();
    }

    @Bean
    ModuleService moduleService(ApplicationContext ctx, IModuleRepository moduleRepo,
                                              IMarketDataRepository mdRepo, AccountManager accountMgr, ModuleManager moduleMgr,
                                              IContractManager contractMgr, MailDeliveryManager mailMgr, GatewayMetaProvider gatewayMetaPvd) {
        return new ModuleService(ctx, moduleRepo, mailMgr, mdRepo, moduleMgr, contractMgr, accountMgr, gatewayMetaPvd);
    }

    @Bean
    LogService logService(LoggingSystem loggingSystem) {
        return new LogService(loggingSystem);
    }
	
}
