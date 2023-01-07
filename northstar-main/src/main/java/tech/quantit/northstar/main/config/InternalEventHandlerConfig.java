package tech.quantit.northstar.main.config;

import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.account.TradeDayAccountFactory;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.main.handler.internal.AccountHandler;
import tech.quantit.northstar.main.handler.internal.ConnectionHandler;
import tech.quantit.northstar.main.handler.internal.MailBindedEventHandler;
import tech.quantit.northstar.main.handler.internal.MarketDataHandler;
import tech.quantit.northstar.main.handler.internal.ModuleManager;
import tech.quantit.northstar.main.handler.internal.SimMarketHandler;
import tech.quantit.northstar.main.mail.MailDeliveryManager;

@Slf4j
@Configuration
public class InternalEventHandlerConfig {
	
	///////////////////
	/* Internal类事件 */
	///////////////////
	@Bean
	public AccountHandler accountEventHandler(InternalEventBus eventBus, IContractManager contractMgr,
			ConcurrentMap<String, TradeDayAccount> accountMap, GatewayAndConnectionManager gatewayConnMgr) {
		AccountHandler handler = new AccountHandler(accountMap, new TradeDayAccountFactory(gatewayConnMgr, contractMgr));
		log.debug("注册：AccountHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ConnectionHandler connectionEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			IContractManager contractMgr, IGatewayRepository gatewayRepo) {
		ConnectionHandler handler = new ConnectionHandler(gatewayConnMgr, contractMgr, gatewayRepo);
		log.debug("注册：ConnectionHandler");
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
	
	@Bean
	public ModuleManager moduleManager(InternalEventBus eventBus) {
		ModuleManager moduleMgr = new ModuleManager();
		log.debug("注册：ModuleManager");
		eventBus.register(moduleMgr);
		return moduleMgr;
	}
	
	@Bean 
	public MarketDataHandler marketDataHandler(IMarketDataRepository mdRepo, InternalEventBus eventBus) {
		MarketDataHandler mdHandler = new MarketDataHandler(mdRepo);
		log.debug("注册：MarketDataHandler");
		eventBus.register(mdHandler);
		return mdHandler;
	}
	
	@Bean
	public MailBindedEventHandler mailBindedEventHandler(MailDeliveryManager mailMgr, InternalEventBus eventBus) {
		MailBindedEventHandler handler = new MailBindedEventHandler(mailMgr);
		log.debug("注册：MailBindedEventHandler");
		eventBus.register(handler);
		return handler;
	}
	//////////////////////
	/* Internal类事件结束 */
	//////////////////////
	
}
