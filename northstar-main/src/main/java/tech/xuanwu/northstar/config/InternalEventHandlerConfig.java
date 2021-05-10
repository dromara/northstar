package tech.xuanwu.northstar.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import tech.xuanwu.northstar.factories.TradeDayAccountFactory;
import tech.xuanwu.northstar.handler.AccountEventHandler;
import tech.xuanwu.northstar.handler.ConnectionEventHandler;
import tech.xuanwu.northstar.handler.ContractEventHandler;
import tech.xuanwu.northstar.handler.TradeEventHandler;
import tech.xuanwu.northstar.model.ContractManager;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;

@Configuration
public class InternalEventHandlerConfig {
	
	@Value("${northstar.contracts.canHandle}")
	private String[] productClassTypes;
	
	@Bean
	public GatewayAndConnectionManager createGatewayAndConnectionManager() {
		return new GatewayAndConnectionManager();
	}
	
	@Bean
	public ContractManager createContractManager() {
		return new ContractManager(productClassTypes);
	}
	
	@Bean
	public ConcurrentHashMap<String, TradeDayAccount> createAccountMap(){
		return new ConcurrentHashMap<>();
	}

	@Bean
	public AccountEventHandler createAccountEventHandler(InternalEventBus eventBus, ContractManager contractMgr,
			ConcurrentHashMap<String, TradeDayAccount> accountMap) {
		AccountEventHandler handler = new AccountEventHandler(accountMap, new TradeDayAccountFactory(eventBus, contractMgr));
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ContractEventHandler createContractEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr) {
		ContractEventHandler handler = new ContractEventHandler(contractMgr, gatewayConnMgr);
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ConnectionEventHandler createConnectionEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr) {
		ConnectionEventHandler handler = new ConnectionEventHandler(gatewayConnMgr, contractMgr);
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public TradeEventHandler createTradeEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr) {
		TradeEventHandler handler = new TradeEventHandler(gatewayConnMgr);
		eventBus.register(handler);
		return handler;
	}
}
