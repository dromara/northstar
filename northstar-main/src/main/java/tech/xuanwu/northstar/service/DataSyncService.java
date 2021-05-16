package tech.xuanwu.northstar.service;

import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.domain.ContractManager;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class DataSyncService {

	private ContractManager contractMgr;
	
	private SocketIOMessageEngine msgEngine;
	
	private ConcurrentHashMap<String, TradeDayAccount> accountMap;
	
	public DataSyncService(ContractManager contractMgr, SocketIOMessageEngine msgEngine,
			ConcurrentHashMap<String, TradeDayAccount> accountMap) {
		this.contractMgr = contractMgr;
		this.msgEngine = msgEngine;
		this.accountMap = accountMap;
	}
	
	/**
	 * 异步更新合约
	 * @throws Exception 
	 */
	public void asyncUpdateContracts() throws Exception {
		log.info("异步更新合约");
		NorthstarEvent event = new NorthstarEvent(null, null);
		for(ContractField c : contractMgr.getAllContracts()) {
			event.setData(c);
			event.setEvent(NorthstarEventType.CONTRACT);
			msgEngine.emitEvent(event, ContractField.class);
		}
	}
	
	/**
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * 
	 */
	public void asyncUpdateTradeAccount() throws Exception {
		log.info("异步更新账户信息");
		NorthstarEvent event = new NorthstarEvent(null, null);
		for(Entry<String, TradeDayAccount> e : accountMap.entrySet()) {
			TradeDayAccount account = e.getValue();
			event.setData(account.getAccountInfo());
			event.setEvent(NorthstarEventType.ACCOUNT);
			msgEngine.emitEvent(event, AccountField.class);
			
			event.setEvent(NorthstarEventType.POSITION);
			for(PositionField pf : account.getPositions()) {
				event.setData(pf);
				msgEngine.emitEvent(event, PositionField.class);
			}
			
			event.setEvent(NorthstarEventType.ORDER);
			for(OrderField of : account.getTradeDayOrders()) {
				event.setData(of);
				msgEngine.emitEvent(event, OrderField.class);
			}
			
			event.setEvent(NorthstarEventType.TRADE);
			for(TradeField tf : account.getTradeDayTransactions()) {
				event.setData(tf);
				msgEngine.emitEvent(event, TradeField.class);
			}
		}
	}
}
