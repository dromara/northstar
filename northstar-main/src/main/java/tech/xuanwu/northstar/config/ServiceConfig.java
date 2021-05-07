package tech.xuanwu.northstar.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import tech.xuanwu.northstar.engine.event.EventEngine;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import tech.xuanwu.northstar.persistence.GatewayRepository;
import tech.xuanwu.northstar.service.AccountService;
import tech.xuanwu.northstar.service.GatewayService;

@DependsOn({
	"createInternalHandler",
	"createPluginHandler",
	"createStrategyHandler",
	"createBroadcastEventHandler",
	"createAccountEventHandler",
	"createContractEventHandler",
	"createConnectionEventHandler",
	"createTradeEventHandler"})
@Configuration
public class ServiceConfig {

	@Bean
	public AccountService createAccountService(ConcurrentHashMap<String, TradeDayAccount> accountMap) {
		return new AccountService(accountMap);
	}
	
	
	@Bean
	public GatewayService createGatewayService(GatewayAndConnectionManager gatewayConnMgr, GatewayRepository gatewayRepo,
			EventEngine eventEngine, InternalEventBus eventBus) {
		return new GatewayService(gatewayConnMgr, gatewayRepo, eventEngine, eventBus);
	}
}
