package tech.xuanwu.northstar.config;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.domain.ContractManager;
import tech.xuanwu.northstar.domain.account.TradeDayAccount;
import tech.xuanwu.northstar.engine.index.IndexEngine;
import tech.xuanwu.northstar.factories.TradeDayAccountFactory;
import tech.xuanwu.northstar.handler.internal.AccountHandler;
import tech.xuanwu.northstar.handler.internal.ConnectionHandler;
import tech.xuanwu.northstar.handler.internal.ContractHandler;
import tech.xuanwu.northstar.handler.internal.TradeHandler;
import tech.xuanwu.northstar.manager.GatewayAndConnectionManager;
import tech.xuanwu.northstar.persistence.MarketDataRepository;

@Slf4j
@Configuration
public class InternalEventHandlerConfig {
	
	///////////////////
	/* Internal类事件 */
	///////////////////
	@Bean
	public AccountHandler accountEventHandler(InternalEventBus eventBus, ContractManager contractMgr,
			ConcurrentHashMap<String, TradeDayAccount> accountMap) {
		AccountHandler handler = new AccountHandler(accountMap, new TradeDayAccountFactory(eventBus, contractMgr));
		log.info("注册：AccountHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ContractHandler contractEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr, IndexEngine idxEngine, MarketDataRepository mdRepo) {
		ContractHandler handler = new ContractHandler(contractMgr, gatewayConnMgr, idxEngine, mdRepo);
		log.info("注册：ContractHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ConnectionHandler connectionEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr) {
		ConnectionHandler handler = new ConnectionHandler(gatewayConnMgr, contractMgr);
		log.info("注册：ConnectionHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public TradeHandler tradeEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr) {
		TradeHandler handler = new TradeHandler(gatewayConnMgr);
		log.info("注册：TradeHandler");
		eventBus.register(handler);
		return handler;
	}
	//////////////////////
	/* Internal类事件结束 */
	//////////////////////
	
}
