package tech.xuanwu.northstar.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.event.MarketDataEventBus;
import tech.xuanwu.northstar.domain.ContractManager;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.factories.TradeDayAccountFactory;
import tech.xuanwu.northstar.handler.data.IndexContractHandler;
import tech.xuanwu.northstar.handler.data.MarketDataHandler;
import tech.xuanwu.northstar.handler.internal.AccountHandler;
import tech.xuanwu.northstar.handler.internal.ConnectionHandler;
import tech.xuanwu.northstar.handler.internal.ContractHandler;
import tech.xuanwu.northstar.handler.internal.TradeHandler;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import tech.xuanwu.northstar.persistence.MarketDataRepository;

@Slf4j
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
	
	///////////////////
	/* Internal类事件 */
	///////////////////
	@Bean
	public AccountHandler createAccountEventHandler(InternalEventBus eventBus, ContractManager contractMgr,
			ConcurrentHashMap<String, TradeDayAccount> accountMap) {
		AccountHandler handler = new AccountHandler(accountMap, new TradeDayAccountFactory(eventBus, contractMgr));
		log.info("注册：AccountHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ContractHandler createContractEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr) {
		ContractHandler handler = new ContractHandler(contractMgr, gatewayConnMgr);
		log.info("注册：ContractHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ConnectionHandler createConnectionEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr) {
		ConnectionHandler handler = new ConnectionHandler(gatewayConnMgr, contractMgr);
		log.info("注册：ConnectionHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public TradeHandler createTradeEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr) {
		TradeHandler handler = new TradeHandler(gatewayConnMgr);
		log.info("注册：TradeHandler");
		eventBus.register(handler);
		return handler;
	}
	//////////////////////
	/* Internal类事件结束 */
	//////////////////////
	
	//////////////////////
	/* MarketData类事件 */
	/////////////////////
	@Bean
	public IndexContractHandler createIndexContractHandler(MarketDataEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr, FastEventEngine fastEventEngine, SocketIOMessageEngine msgEngine) {
		IndexContractHandler handler = new IndexContractHandler(gatewayConnMgr, contractMgr, fastEventEngine, msgEngine);
		log.info("注册：IndexContractHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public MarketDataHandler createMarketDataHandler(MarketDataEventBus eventBus, FastEventEngine feEngine, MarketDataRepository mdRepo) {
		MarketDataHandler handler = new MarketDataHandler(feEngine, mdRepo);
		log.info("注册：MarketDataHandler");
		eventBus.register(handler);
		return handler;
	}
	////////////////////////
	/* MarketData类事件结束 */
	////////////////////////
}
