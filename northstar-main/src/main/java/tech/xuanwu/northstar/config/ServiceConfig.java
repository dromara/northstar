package tech.xuanwu.northstar.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.domain.ContractManager;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.gateway.sim.SimMarket;
import tech.xuanwu.northstar.gateway.sim.persistence.SimAccountRepository;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import tech.xuanwu.northstar.persistence.GatewayRepository;
import tech.xuanwu.northstar.persistence.MarketDataRepository;
import tech.xuanwu.northstar.service.AccountService;
import tech.xuanwu.northstar.service.DataSyncService;
import tech.xuanwu.northstar.service.GatewayService;

@DependsOn({
	"createInternalDispatcher",
	"createBroadcastEventDispatcher",
//	"createPluginHandler",
//	"createStrategyHandler",
	"createAccountEventHandler",
	"createContractEventHandler",
	"createConnectionEventHandler",
	"createTradeEventHandler"
	})
@Configuration
public class ServiceConfig {

	@Bean
	public AccountService createAccountService(ConcurrentHashMap<String, TradeDayAccount> accountMap) {
		return new AccountService(accountMap);
	}
	
	
	@Bean
	public GatewayService createGatewayService(GatewayAndConnectionManager gatewayConnMgr, GatewayRepository gatewayRepo,
			MarketDataRepository mdRepo, FastEventEngine fastEventEngine, InternalEventBus eventBus, SimMarket simMarket,
			SimAccountRepository simAccRepo, ContractManager contractMgr) {
		return new GatewayService(gatewayConnMgr, gatewayRepo, mdRepo, fastEventEngine, eventBus, simMarket, simAccRepo, contractMgr);
	}
	
	@Bean
	public DataSyncService createDataSyncService(ContractManager contractMgr, SocketIOMessageEngine msgEngine, 
			ConcurrentHashMap<String, TradeDayAccount> accountMap) {
		return new DataSyncService(contractMgr, msgEngine, accountMap);
	}
}
