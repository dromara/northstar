package org.dromara.northstar.config;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.event.AccountHandler;
import org.dromara.northstar.event.BroadcastHandler;
import org.dromara.northstar.event.ConnectionHandler;
import org.dromara.northstar.event.MailBindedEventHandler;
import org.dromara.northstar.event.MarketDataHandler;
import org.dromara.northstar.event.ModuleManager;
import org.dromara.northstar.event.SimMarketHandler;
import org.dromara.northstar.gateway.api.IContractManager;
import org.dromara.northstar.gateway.api.utils.MarketDataRepoFactory;
import org.dromara.northstar.gateway.sim.trade.SimMarket;
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
	SimMarketHandler simMarketHandler(SimMarket market) {
		log.debug("注册：SimMarketHandler");
		return new SimMarketHandler(market);
	}
	
	@Bean
	ModuleManager moduleManager() {
		log.debug("注册：ModuleManager");
		return new ModuleManager();
	}
	
	@Bean 
	MarketDataHandler marketDataHandler(MarketDataRepoFactory mdRepoFactory) {
		log.debug("注册：MarketDataHandler");
		return new MarketDataHandler(mdRepoFactory);
	}
	
	@Bean
	MailBindedEventHandler mailBindedEventHandler(MailDeliveryManager mailMgr) {
		log.debug("注册：MailBindedEventHandler");
		return new MailBindedEventHandler(mailMgr);
	}
	
}
