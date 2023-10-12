package org.dromara.northstar.config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.IMessageSenderRepository;
import org.dromara.northstar.event.AccountHandler;
import org.dromara.northstar.event.BroadcastHandler;
import org.dromara.northstar.event.ConnectionHandler;
import org.dromara.northstar.event.EventNotificationHandler;
import org.dromara.northstar.event.IllegalOrderHandler;
import org.dromara.northstar.event.MarketDataHandler;
import org.dromara.northstar.event.ModuleHandler;
import org.dromara.northstar.event.SimMarketHandler;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.IMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
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
	EventNotificationHandler eventNotificationHandler(@Autowired(required = false) IMessageSender sender, Set<NorthstarEventType> subEvents) {
		log.debug("注册：EventNotificationHandler");
		return new EventNotificationHandler(sender, subEvents);
	}
	
	@Bean
	IllegalOrderHandler illegalOrderHandler() {
		log.debug("注册：IllegalOrderHandler");
		return new IllegalOrderHandler();
	}
	
	@Bean
	Set<NorthstarEventType> notificationEvents(IMessageSenderRepository repo){
		List<NorthstarEventType> records = repo.getSubEvents();
		if(repo.getSubEvents() != null) {
			return records.stream().collect(Collectors.toSet());
		}
		return Set.of(NorthstarEventType.LOGGED_IN, NorthstarEventType.LOGGED_OUT, NorthstarEventType.ORDER, NorthstarEventType.TRADE, NorthstarEventType.NOTICE);
	}
}
