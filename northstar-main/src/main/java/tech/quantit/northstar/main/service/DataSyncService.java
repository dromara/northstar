package tech.quantit.northstar.main.service;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.SimpleContractInfo;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.main.handler.broadcast.SocketIOMessageEngine;
import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class DataSyncService {

	private ContractManager contractMgr;
	
	private SocketIOMessageEngine msgEngine;
	
	private ConcurrentMap<String, TradeDayAccount> accountMap;
	
	private MarketDataRepository mdRepo;
	
	public DataSyncService(ContractManager contractMgr, SocketIOMessageEngine msgEngine, MarketDataRepository mdRepo,
			ConcurrentMap<String, TradeDayAccount> accountMap) {
		this.contractMgr = contractMgr;
		this.msgEngine = msgEngine;
		this.accountMap = accountMap;
		this.mdRepo = mdRepo;
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
			msgEngine.emitEvent(event);
		}
	}
	
	/**
	 * 异步更新账户信息
	 * @throws Exception 
	 * 
	 */
	public void asyncUpdateTradeAccount() throws Exception {
		log.info("异步更新账户信息");
		for(Entry<String, TradeDayAccount> e : accountMap.entrySet()) {
			TradeDayAccount account = e.getValue();
			msgEngine.emitEvent(new NorthstarEvent(NorthstarEventType.ACCOUNT, account.getAccountInfo()));
			
			for(PositionField pf : account.getPositions()) {
				msgEngine.emitEvent(new NorthstarEvent(NorthstarEventType.POSITION, pf));
			}
			
			for(OrderField of : account.getTradeDayOrders()) {
				msgEngine.emitEvent(new NorthstarEvent(NorthstarEventType.ORDER, of));
			}
			
			for(TradeField tf : account.getTradeDayTransactions()) {
				msgEngine.emitEvent(new NorthstarEvent(NorthstarEventType.TRADE, tf));
			}
		}
	}
	
	/**
	 * 异步加载历史Bar数据
	 * @throws InvalidProtocolBufferException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws Exception 
	 */
	public List<byte[]> loadHistoryBarData(String gatewayId, String unifiedSymbol, LocalDate startDate, LocalDate endDate) throws InvalidProtocolBufferException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// 自动处理起止日期反转的情况
		if(startDate.isAfter(endDate)) {
			LocalDate tmpDate = startDate;
			startDate = endDate;
			endDate = tmpDate;
		}
		LocalDate curDate = startDate;
		List<byte[]> results = new LinkedList<>();
		while(!curDate.isAfter(endDate)) {
			String date = curDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
			List<MinBarDataPO> dayBars = mdRepo.loadDataByDate(gatewayId, unifiedSymbol, date);
			for(MinBarDataPO po : dayBars) {
				results.add(po.getBarData());
			}
			
			curDate = curDate.plusDays(1);
		}
		
		return results;
	}
	
	/**
	 * 获取可用合约
	 * @return
	 */
	public List<SimpleContractInfo> getAvailableContracts(){
		Collection<ContractField> resultList = contractMgr.getAllContracts();
		return resultList.stream()
				.map(cf -> new SimpleContractInfo(cf.getUnifiedSymbol(), cf.getName(), cf.getGatewayId()))
				.toList();
	}
}
