package org.dromara.northstar.config;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.event.AccountHandler;
import org.dromara.northstar.event.BarAggregationHandler;
import org.dromara.northstar.event.ConnectionHandler;
import org.dromara.northstar.event.EventNotificationHandler;
import org.dromara.northstar.event.IllegalOrderHandler;
import org.dromara.northstar.event.MarketDataPersistanceHandler;
import org.dromara.northstar.event.ModuleHandler;
import org.dromara.northstar.event.PlannedTradeHandler;
import org.dromara.northstar.event.SimMarketHandler;
import org.dromara.northstar.event.SseBroadcastHandler;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.playback.GlobalPlaybackDataDetector;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.dromara.northstar.support.utils.MessageSenderFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
class InternalEventHandlerConfig {
	
	@Bean
	AccountHandler accountEventHandler(AccountManager accountMgr) {
		log.debug("注册：AccountHandler");
		return new AccountHandler(accountMgr);
	}
	
	@Bean
	SseBroadcastHandler broadcastEventHandler(EventEmitterHolder emitterHolder) {
		log.debug("注册：BroadcastHandler");
		return new SseBroadcastHandler(emitterHolder);
	}
	
	@Bean
	ConnectionHandler connectionEventHandler(GatewayManager gatewayMgr, IContractManager contractMgr, IGatewayRepository gatewayRepo) {
		log.debug("注册：ConnectionHandler");
		return new ConnectionHandler(gatewayMgr, contractMgr, gatewayRepo);
	}
	
	@Bean
	SimMarketHandler simMarketHandler(GatewayManager gatewayMgr, AccountManager accountMgr) {
		log.debug("注册：SimMarketHandler");
		return new SimMarketHandler(gatewayMgr, accountMgr);
	}
	
	@Bean
	ModuleHandler moduleHandler(ModuleManager moduleMgr) {
		log.debug("注册：ModuleManager");
		return new ModuleHandler(moduleMgr);
	}
	
	@Bean 
	MarketDataPersistanceHandler marketDataHandler(IMarketDataRepository mdRepo) {
		log.debug("注册：MarketDataHandler");
		return new MarketDataPersistanceHandler(mdRepo);
	}
	
	@Bean
	EventNotificationHandler eventNotificationHandler(MessageSenderFacade sender) {
		log.debug("注册：EventNotificationHandler");
		return new EventNotificationHandler(sender);
	}
	
	@Bean
	IllegalOrderHandler illegalOrderHandler() {
		log.debug("注册：IllegalOrderHandler");
		return new IllegalOrderHandler();
	}
	
	@Bean
	BarAggregationHandler barAggregationHandler(EventEmitterHolder emitterHolder, GatewayManager gatewayMgr, GlobalPlaybackDataDetector dataDetector) {
		log.debug("注册：barAggregationHandler");
		return new BarAggregationHandler(emitterHolder, gatewayMgr, dataDetector);
	}
	
	@Bean
	PlannedTradeHandler plannedTradeHandler(EventEmitterHolder emitterHolder, AccountManager accMgr, MarketCenter mktCenter) {
		log.debug("注册：plannedTradeHandler");
		return new PlannedTradeHandler(accMgr, mktCenter, emitterHolder);
	}
	
}
