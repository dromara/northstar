package tech.xuanwu.northstar.trader.domain.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.constant.GatewayLifecycleEvent;
import tech.xuanwu.northstar.gateway.FastEventEngine;
import tech.xuanwu.northstar.gateway.FastEventEngine.EventType;
import tech.xuanwu.northstar.gateway.FastEventEngine.FastEvent;
import tech.xuanwu.northstar.gateway.FastEventEngine.FastEventHandler;
import tech.xuanwu.northstar.persistance.AccountRepo;
import tech.xuanwu.northstar.persistance.po.Account;
import tech.xuanwu.northstar.persistance.po.Position;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 负责记录账户与持仓信息
 * @author kevinhuangwl
 *
 */
@Slf4j
@Component
public class AccountInfoRecorder implements FastEventHandler{

	@Autowired
	private FastEventEngine feEngine;
	
	@Autowired
	private AccountRepo accountRepo;
	
	private Map<String, AtomicInteger> accountChangeEventCountMap = new HashMap<>();
	private Map<String, AtomicInteger> positionChangeEventCountMap = new HashMap<>();
	
	private Map<String, Account> accountMap = new HashMap<>();
	private Map<String, Position> positionMap = new HashMap<>();
	private Set<String> gatewayAccountUpdateSet = new HashSet<>();
	
	
	@Autowired
	private ContractProfitRecorder contractProfitRecorder;
	
	public void init() {
		feEngine.addHandler(this);
		List<Account> accountList = accountRepo.findAll();
		for(Account po : accountList) {
			accountMap.put(po.getGatewayId(), po);
			positionMap.putAll(po.getPositionMap());
		}
	}

	@Override
	public void onEvent(FastEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(event.getEventType() != EventType.TRADE && event.getEventType() != EventType.ACCOUNT 
				&& event.getEventType() != EventType.POSITION && event.getEventType() != EventType.BALANCE_CHANGE) {
			return;
		}
		
		if(event.getEventType() == EventType.BALANCE_CHANGE) {
			AccountField account = (AccountField) event.getObj();
			Account po = Account.convertFrom(account);
			accountRepo.save(po);
			return;
		}
		
		if(event.getEventType() == EventType.TRADE) {
			TradeField trade = (TradeField) event.getObj();
			String gatewayId = trade.getGatewayId();
			accountChangeEventCountMap.putIfAbsent(gatewayId, new AtomicInteger(0));
			positionChangeEventCountMap.putIfAbsent(gatewayId, new AtomicInteger(0));
			accountChangeEventCountMap.get(gatewayId).getAndIncrement();
			positionChangeEventCountMap.get(gatewayId).getAndIncrement();
			contractProfitRecorder.record(trade);
			log.info("登记成交");
		}
		
		if(event.getEventType() == EventType.ACCOUNT) {
			AccountField account = (AccountField) event.getObj();
			String gatewayId = account.getGatewayId();
			if(accountChangeEventCountMap.get(gatewayId) != null && accountChangeEventCountMap.get(gatewayId).get() > 0) {
				accountChangeEventCountMap.get(gatewayId).getAndDecrement();
				Account po = accountMap.get(gatewayId);
				if(po == null) {
					po = new Account();
				}
				BeanUtils.copyProperties(account.toBuilder(), po);
				accountMap.put(gatewayId, po);
				gatewayAccountUpdateSet.add(gatewayId);
			}
		}
		
		if(event.getEventType() == EventType.POSITION) {
			PositionField position = (PositionField) event.getObj();
			Position po = positionMap.get(position.getPositionId());
			if(po == null) {
				po = new Position();
			}
			BeanUtils.copyProperties(position.toBuilder(), po);
			po.setContractUnifiedSymbol(position.getContract().getUnifiedSymbol());
			positionMap.put(po.getPositionId(), po);
		}
		
		if(gatewayAccountUpdateSet.size() > 0) {
			for(String gatewayId : gatewayAccountUpdateSet) {
				Account po = accountMap.get(gatewayId);
				Map<String, Position> gatewayPositionMap = new HashMap<>();
				for(Entry<String, Position> e : positionMap.entrySet()) {
					Position p = e.getValue();
					if(!StringUtils.equals(gatewayId, p.getGatewayId())) {
						continue;
					}
					gatewayPositionMap.put(p.getPositionId(), p);
				}
				po.getPositionMap().putAll(gatewayPositionMap);
				accountRepo.save(po);
				log.info("保存账户", po.convertTo().toString());
			}
			gatewayAccountUpdateSet.clear();
		}
	}

}
