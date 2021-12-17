package tech.quantit.northstar.main.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.SimpleContractInfo;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.main.engine.broadcast.SocketIOMessageEngine;
import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;
import tech.quantit.northstar.main.utils.ProtoBeanUtils;
import xyz.redtorch.pb.CoreField.AccountField;
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
			msgEngine.emitEvent(event, ContractField.class);
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
			msgEngine.emitEvent(new NorthstarEvent(NorthstarEventType.ACCOUNT, account.getAccountInfo()), AccountField.class);
			
			for(PositionField pf : account.getPositions()) {
				msgEngine.emitEvent(new NorthstarEvent(NorthstarEventType.POSITION, pf), PositionField.class);
			}
			
			for(OrderField of : account.getTradeDayOrders()) {
				msgEngine.emitEvent(new NorthstarEvent(NorthstarEventType.ORDER, of), OrderField.class);
			}
			
			for(TradeField tf : account.getTradeDayTransactions()) {
				msgEngine.emitEvent(new NorthstarEvent(NorthstarEventType.TRADE, tf), TradeField.class);
			}
		}
	}
	
	/**
	 * 异步加载历史Bar数据
	 * @throws Exception 
	 */
	public void asyncLoadHistoryBarData(String gatewayId, String unifiedSymbol, LocalDate startDate, LocalDate endDate) throws Exception {
		// 自动处理起止日期反转的情况
		if(startDate.isAfter(endDate)) {
			LocalDate tmpDate = startDate;
			startDate = endDate;
			endDate = tmpDate;
		}
		LocalDate curDate = startDate;
		NorthstarEvent ne = new NorthstarEvent(NorthstarEventType.HIS_BAR, null);
		while(!curDate.isAfter(endDate)) {
			String date = curDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
			List<MinBarDataPO> dayBars = mdRepo.loadDataByDate(gatewayId, unifiedSymbol, date);
			for(MinBarDataPO po : dayBars) {
				// 清空冗余属性，避免转换异常
				po.setTicksOfMin(null);
				po.setNumOfTicks(null);
				BarField.Builder bb = BarField.newBuilder();
				ProtoBeanUtils.toProtoBean(bb, po);
				ne.setData(bb.build());
				msgEngine.emitEvent(ne, BarField.class);
			}
			
			curDate = curDate.plusDays(1);
		}
		
		// 历史行情结束信号
		BarField bf = BarField.newBuilder()
				.setGatewayId(gatewayId)
				.setUnifiedSymbol(unifiedSymbol)
				.build();
		ne.setData(bf);
		msgEngine.emitEvent(ne, BarField.class);
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
