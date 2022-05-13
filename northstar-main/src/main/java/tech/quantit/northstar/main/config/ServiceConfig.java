package tech.quantit.northstar.main.config;

import java.util.concurrent.ConcurrentMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.main.ExternalJarListener;
import tech.quantit.northstar.main.handler.internal.ModuleManager;
import tech.quantit.northstar.main.service.AccountService;
import tech.quantit.northstar.main.service.GatewayService;
import tech.quantit.northstar.main.service.ModuleService;

@DependsOn({
	"internalDispatcher",
	"broadcastEventDispatcher",
	"strategyDispatcher",
	"accountEventHandler",
	"connectionEventHandler",
	"ctpGatewayFactory",
	"simGatewayFactory",
	"ctpSimGatewayFactory"
	})
@Configuration
public class ServiceConfig {

	@Bean
	public AccountService accountService(ConcurrentMap<String, TradeDayAccount> accountMap) {
		return new AccountService(accountMap);
	}
	
	@Bean
	public GatewayService gatewayService(GatewayAndConnectionManager gatewayConnMgr, IGatewayRepository gatewayRepo, IMarketDataRepository mdRepo,
			IModuleRepository moduleRepo, GlobalMarketRegistry registry) {
		return new GatewayService(gatewayConnMgr, gatewayRepo, mdRepo, moduleRepo, registry);
	}
	
	@Bean
	public ModuleService moduleService(ApplicationContext ctx, ExternalJarListener extJarListener, IModuleRepository moduleRepo,
			ModuleManager moduleMgr) {
		return new ModuleService(ctx, extJarListener, moduleRepo, moduleMgr);
	}
}
