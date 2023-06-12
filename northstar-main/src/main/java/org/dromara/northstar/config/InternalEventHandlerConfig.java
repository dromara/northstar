package org.dromara.northstar.config;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.event.AccountHandler;
import org.dromara.northstar.event.BroadcastHandler;
import org.dromara.northstar.event.ConnectionHandler;
import org.dromara.northstar.event.IllegalOrderHandler;
import org.dromara.northstar.event.MailBindedEventHandler;
import org.dromara.northstar.event.MarketDataHandler;
import org.dromara.northstar.event.ModuleHandler;
import org.dromara.northstar.event.SimMarketHandler;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.support.notification.MailDeliveryManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketIOServer;

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
	BroadcastHandler broadcastEventHandler(SocketIOServer socketServer) {
		log.debug("注册：BroadcastHandler");
		return new BroadcastHandler(socketServer);
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
	MarketDataHandler marketDataHandler(IMarketDataRepository mdRepo) {
		log.debug("注册：MarketDataHandler");
		return new MarketDataHandler(mdRepo);
	}
	
	@Bean
	MailBindedEventHandler mailBindedEventHandler(MailDeliveryManager mailMgr) {
		log.debug("注册：MailBindedEventHandler");
		return new MailBindedEventHandler(mailMgr);
	}
	
	@Bean
	IllegalOrderHandler illegalOrderHandler() {
		log.debug("注册：IllegalOrderHandler");
		return new IllegalOrderHandler();
	}
	
}
