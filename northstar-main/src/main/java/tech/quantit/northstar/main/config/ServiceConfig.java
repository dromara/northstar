package tech.quantit.northstar.main.config;

import java.util.concurrent.ConcurrentMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import tech.quantit.northstar.common.IMailSender;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.strategy.ModuleManager;
import tech.quantit.northstar.domain.strategy.SandboxModuleManager;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountRepository;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.main.ExternalJarListener;
import tech.quantit.northstar.main.handler.broadcast.SocketIOMessageEngine;
import tech.quantit.northstar.main.persistence.GatewayRepository;
import tech.quantit.northstar.main.persistence.IMarketDataRepository;
import tech.quantit.northstar.main.persistence.ModuleRepository;
import tech.quantit.northstar.main.service.AccountService;
import tech.quantit.northstar.main.service.DataSyncService;
import tech.quantit.northstar.main.service.GatewayService;
import tech.quantit.northstar.main.service.ModuleService;
import tech.quantit.northstar.main.service.PlaybackService;
import tech.quantit.northstar.main.service.SMSTradeService;

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
	public GatewayService gatewayService(GatewayAndConnectionManager gatewayConnMgr, GatewayRepository gatewayRepo,
			IMarketDataRepository mdRepo, ModuleRepository moduleRepo, SimMarket simMarket, GlobalMarketRegistry registry) {
		return new GatewayService(gatewayConnMgr, gatewayRepo, mdRepo, moduleRepo, simMarket, registry);
	}
	
	@Bean
	public DataSyncService dataSyncService(ContractManager contractMgr, SocketIOMessageEngine msgEngine, IMarketDataRepository mdRepo,
			ConcurrentMap<String, TradeDayAccount> accountMap) {
		return new DataSyncService(contractMgr, msgEngine, mdRepo, accountMap);
	}
	
	@Bean
	public ModuleService moduleService(ApplicationContext ctx, ModuleRepository moduleRepo, IMarketDataRepository mdRepo, ExternalJarListener extJarListener,
			ModuleManager mdlMgr, GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr, IMailSender sender) {
		return new ModuleService(ctx, moduleRepo, mdRepo, extJarListener, mdlMgr, gatewayConnMgr, contractMgr, sender);
	}
	
	@Bean
	public SMSTradeService smsTradeService(FastEventEngine feEngine) {
		return new SMSTradeService(feEngine);
	}
	
	@Bean
	public PlaybackService playbackService(FastEventEngine feEngine, SandboxModuleManager moduleMgr, GatewayAndConnectionManager gatewayConnMgr, 
			ContractManager contractMgr, ModuleRepository moduleRepo, IMarketDataRepository mdRepo, SimMarket simMarket, SimAccountRepository simAccRepo,
			GlobalMarketRegistry registry, ExternalJarListener extJarListener) {
		return new PlaybackService(feEngine, moduleMgr, gatewayConnMgr, contractMgr, moduleRepo, mdRepo, simMarket, simAccRepo, registry, extJarListener);
	}
}
