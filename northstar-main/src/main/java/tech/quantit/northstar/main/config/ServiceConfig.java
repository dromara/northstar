package tech.quantit.northstar.main.config;

import java.util.concurrent.ConcurrentMap;

import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMailConfigRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.gateway.api.GatewaySettingsMetaInfoProvider;
import tech.quantit.northstar.gateway.api.GatewayChannelProvider;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.main.ExternalJarClassLoader;
import tech.quantit.northstar.main.handler.internal.ModuleManager;
import tech.quantit.northstar.main.mail.MailDeliveryManager;
import tech.quantit.northstar.main.service.AccountService;
import tech.quantit.northstar.main.service.EmailConfigService;
import tech.quantit.northstar.main.service.GatewayService;
import tech.quantit.northstar.main.service.LogService;
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
	"playbackGatewayFactory",
	"moduleFactory",
	})
@Configuration
public class ServiceConfig {

	@Bean
	public AccountService accountService(ConcurrentMap<String, TradeDayAccount> accountMap) {
		return new AccountService(accountMap);
	}
	
	@Bean
	public GatewayService gatewayService(GatewayAndConnectionManager gatewayConnMgr, IGatewayRepository gatewayRepo, IMarketDataRepository mdRepo,
			IPlaybackRuntimeRepository playbackRtRepo, IModuleRepository moduleRepo, ISimAccountRepository simAccRepo, GatewayChannelProvider gtp,
			GatewaySettingsMetaInfoProvider settingsPvd, IContractManager contractMgr) {
		return new GatewayService(gatewayConnMgr, settingsPvd, gtp, contractMgr, gatewayRepo, mdRepo, simAccRepo, playbackRtRepo, moduleRepo);
	}
	
	@Bean
	public ModuleService moduleService(ApplicationContext ctx, ExternalJarClassLoader extJarLoader, IModuleRepository moduleRepo, 
			IMarketDataRepository mdRepo,IGatewayRepository gatewayRepo, ModuleFactory moduleFactory, ModuleManager moduleMgr, 
			IContractManager contractMgr) {
		return new ModuleService(ctx, extJarLoader, gatewayRepo, moduleRepo, mdRepo, moduleFactory, moduleMgr, contractMgr);
	}
	
	@Bean
	public EmailConfigService emailConfigService(MailDeliveryManager mailMgr, IMailConfigRepository repo) {
		return new EmailConfigService(mailMgr, repo);
	}
	
	@Bean
	public LogService logService(LoggingSystem loggingSystem) {
		return new LogService(loggingSystem);
	}
	
}
