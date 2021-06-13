package tech.xuanwu.northstar.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.domain.ContractManager;
import tech.xuanwu.northstar.domain.account.TradeDayAccount;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.sim.SimMarket;
import tech.xuanwu.northstar.gateway.sim.persistence.SimAccountRepository;
import tech.xuanwu.northstar.manager.GatewayAndConnectionManager;
import tech.xuanwu.northstar.manager.ModuleManager;
import tech.xuanwu.northstar.persistence.GatewayRepository;
import tech.xuanwu.northstar.persistence.MarketDataRepository;
import tech.xuanwu.northstar.persistence.ModuleRepository;
import tech.xuanwu.northstar.service.AccountService;
import tech.xuanwu.northstar.service.ModuleService;
import tech.xuanwu.northstar.service.DataSyncService;
import tech.xuanwu.northstar.service.GatewayService;

@DependsOn({
	"internalDispatcher",
	"broadcastEventDispatcher",
	"pluginDispatcher",
	"strategyDispatcher",
	"accountEventHandler",
	"contractEventHandler",
	"connectionEventHandler",
	"tradeEventHandler"
	})
@Configuration
public class ServiceConfig {

	@Bean
	public AccountService accountService(ConcurrentHashMap<String, TradeDayAccount> accountMap) {
		return new AccountService(accountMap);
	}
	
	
	@Bean
	public GatewayService gatewayService(GatewayAndConnectionManager gatewayConnMgr, GatewayRepository gatewayRepo,
			MarketDataRepository mdRepo, FastEventEngine fastEventEngine, InternalEventBus eventBus, SimMarket simMarket,
			SimAccountRepository simAccRepo, ContractManager contractMgr) {
		return new GatewayService(gatewayConnMgr, gatewayRepo, mdRepo, fastEventEngine, eventBus, simMarket, simAccRepo, contractMgr);
	}
	
	@Bean
	public DataSyncService dataSyncService(ContractManager contractMgr, SocketIOMessageEngine msgEngine, MarketDataRepository mdRepo,
			ConcurrentHashMap<String, TradeDayAccount> accountMap) {
		return new DataSyncService(contractMgr, msgEngine, mdRepo, accountMap);
	}
	
	@Bean
	public ModuleService moduleService(ApplicationContext ctx, ModuleRepository moduleRepo, MarketDataRepository mdRepo,
			ModuleManager mdlMgr, GatewayAndConnectionManager gatewayConnMgr) {
		return new ModuleService(ctx, moduleRepo, mdRepo, mdlMgr, gatewayConnMgr);
	}
}
