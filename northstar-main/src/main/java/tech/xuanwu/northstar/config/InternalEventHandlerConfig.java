package tech.xuanwu.northstar.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import tech.xuanwu.northstar.factories.TradeDayAccountFactory;
import tech.xuanwu.northstar.handler.AccountHandler;
import tech.xuanwu.northstar.handler.ConnectionHandler;
import tech.xuanwu.northstar.handler.ContractHandler;
import tech.xuanwu.northstar.handler.TradeHandler;
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
	public AccountHandler createAccountEventHandler(InternalEventBus eventBus, ContractManager contractMgr,
			ConcurrentHashMap<String, TradeDayAccount> accountMap) {
		AccountHandler handler = new AccountHandler(accountMap, new TradeDayAccountFactory(eventBus, contractMgr));
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ContractHandler createContractEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr) {
		ContractHandler handler = new ContractHandler(contractMgr, gatewayConnMgr);
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ConnectionHandler createConnectionEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr) {
		ConnectionHandler handler = new ConnectionHandler(gatewayConnMgr, contractMgr);
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public TradeHandler createTradeEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr) {
		TradeHandler handler = new TradeHandler(gatewayConnMgr);
		eventBus.register(handler);
		return handler;
	}
}
