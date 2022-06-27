package tech.quantit.northstar.main.config;

import java.util.concurrent.ConcurrentMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.main.ExternalJarClassLoader;
import tech.quantit.northstar.main.handler.internal.ModuleManager;
import tech.quantit.northstar.main.service.AccountService;
import tech.quantit.northstar.main.service.GatewayService;
import tech.quantit.northstar.main.service.ModuleService;
import tech.quantit.northstar.main.utils.ModuleFactory;

@DependsOn({
	"internalDispatcher",
	"broadcastEventDispatcher",
	"strategyDispatcher",
	"accountEventHandler",
	"connectionEventHandler",
	"ctpGatewayFactory",
	"simGatewayFactory",
	"ctpSimGatewayFactory",
	"moduleFactory",
	"globalRegistry",
	"contractManager"
	})
@Configuration
public class ServiceConfig {

	@Bean
	public AccountService accountService(ConcurrentMap<String, TradeDayAccount> accountMap) {
		return new AccountService(accountMap);
	}
	
	@Bean
	public GatewayService gatewayService(GatewayAndConnectionManager gatewayConnMgr, IGatewayRepository gatewayRepo, IMarketDataRepository mdRepo,
			IModuleRepository moduleRepo, ISimAccountRepository simAccRepo, ContractManager contractMgr) {
		return new GatewayService(gatewayConnMgr, gatewayRepo, mdRepo, moduleRepo, simAccRepo, contractMgr);
	}
	
	@Bean
	public ModuleService moduleService(ApplicationContext ctx, ExternalJarClassLoader extJarLoader, IModuleRepository moduleRepo, IMarketDataRepository mdRepo,
			ModuleFactory moduleFactory, ModuleManager moduleMgr, ContractManager contractMgr) {
		return new ModuleService(ctx, extJarLoader, moduleRepo, mdRepo, moduleFactory, moduleMgr, contractMgr);
	}
	
}
