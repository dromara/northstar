package tech.quantit.northstar.main.config;

import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.domain.account.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.main.factories.TradeDayAccountFactory;
import tech.quantit.northstar.main.handler.internal.AccountHandler;
import tech.quantit.northstar.main.handler.internal.ConnectionHandler;
import tech.quantit.northstar.main.handler.internal.SimMarketHandler;
import tech.quantit.northstar.main.handler.internal.TradeHandler;

@Slf4j
@Configuration
public class InternalEventHandlerConfig {
	
	///////////////////
	/* Internal类事件 */
	///////////////////
	@Bean
	public AccountHandler accountEventHandler(InternalEventBus eventBus, ContractManager contractMgr,
			ConcurrentMap<String, TradeDayAccount> accountMap) {
		AccountHandler handler = new AccountHandler(accountMap, new TradeDayAccountFactory(eventBus, contractMgr));
		log.debug("注册：AccountHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ConnectionHandler connectionEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr) {
		ConnectionHandler handler = new ConnectionHandler(gatewayConnMgr, contractMgr);
		log.debug("注册：ConnectionHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public TradeHandler tradeEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr) {
		TradeHandler handler = new TradeHandler(gatewayConnMgr);
		log.debug("注册：TradeHandler");
		eventBus.register(handler);
		return handler;
	}
	
	
	@Bean
	public SimMarketHandler simMarketHandler(InternalEventBus eventBus, SimMarket market) {
		SimMarketHandler handler = new SimMarketHandler(market);
		log.debug("注册：SimMarketHandler");
		eventBus.register(handler);
		return handler;
	}
	//////////////////////
	/* Internal类事件结束 */
	//////////////////////
	
}
