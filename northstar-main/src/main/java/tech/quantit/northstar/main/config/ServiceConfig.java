package tech.quantit.northstar.main.config;

import java.util.concurrent.ConcurrentMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.domain.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.strategy.ModuleManager;
import tech.quantit.northstar.domain.strategy.SandboxModuleManager;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountRepository;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.main.engine.broadcast.SocketIOMessageEngine;
import tech.quantit.northstar.main.persistence.GatewayRepository;
import tech.quantit.northstar.main.persistence.MarketDataRepository;
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
	"tradeEventHandler",
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
			MarketDataRepository mdRepo, ModuleRepository moduleRepo, InternalEventBus eventBus, SimMarket simMarket, GlobalMarketRegistry registry) {
		return new GatewayService(gatewayConnMgr, gatewayRepo, mdRepo, moduleRepo, eventBus, simMarket, registry);
	}
	
	@Bean
	public DataSyncService dataSyncService(ContractManager contractMgr, SocketIOMessageEngine msgEngine, MarketDataRepository mdRepo,
			ConcurrentMap<String, TradeDayAccount> accountMap) {
		return new DataSyncService(contractMgr, msgEngine, mdRepo, accountMap);
	}
	
	@Bean
	public ModuleService moduleService(ApplicationContext ctx, ModuleRepository moduleRepo, MarketDataRepository mdRepo,
			ModuleManager mdlMgr, GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr) {
		return new ModuleService(ctx, moduleRepo, mdRepo, mdlMgr, gatewayConnMgr, contractMgr);
	}
	
	@Bean
	public SMSTradeService smsTradeService(FastEventEngine feEngine) {
		return new SMSTradeService(feEngine);
	}
	
	@Bean
	public PlaybackService playbackService(FastEventEngine feEngine, SandboxModuleManager moduleMgr, GatewayAndConnectionManager gatewayConnMgr, 
			ContractManager contractMgr, ModuleRepository moduleRepo, MarketDataRepository mdRepo, SimMarket simMarket, SimAccountRepository simAccRepo,
			GlobalMarketRegistry registry) {
		return new PlaybackService(feEngine, moduleMgr, gatewayConnMgr, contractMgr, moduleRepo, mdRepo, simMarket, simAccRepo, registry);
	}
}
