package tech.xuanwu.northstar.trader.domain.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
@Component
public class AccountInfoRecorder implements FastEventHandler{

	@Autowired
	private FastEventEngine feEngine;
	
	@Autowired
	private AccountRepo accountRepo;
	
	private int accountWaitForEventCount;
	private int positionWaitForEventCount;
	private boolean saveFlag;
	
	private Map<String, Account> accountMap = new HashMap<>();
	private Map<String, Position> positionMap = new HashMap<>();
	private Set<String> gatewayAccountUpdateSet = new HashSet<>();
	
	@Autowired
	private ContractProfitRecorder contractProfitRecorder;
	
	@PostConstruct
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
		if(event.getEventType() == EventType.TRADE) {
			saveFlag = true;
			accountWaitForEventCount++;
			positionWaitForEventCount++;
			TradeField trade = (TradeField) event.getObj();
			contractProfitRecorder.record(trade);
		}
		
		if(saveFlag && accountWaitForEventCount > 0 && event.getEventType() == EventType.ACCOUNT) {
			accountWaitForEventCount--;
			AccountField account = (AccountField) event.getObj();
			Account po = accountMap.get(account.getGatewayId());
			BeanUtils.copyProperties(account.toBuilder(), po);
			gatewayAccountUpdateSet.add(account.getGatewayId());
		}
		
		if(saveFlag && positionWaitForEventCount > 0 && event.getEventType() == EventType.POSITION) {
			positionWaitForEventCount--;
			PositionField position = (PositionField) event.getObj();
			Position po = positionMap.get(position.getPositionId());
			BeanUtils.copyProperties(position.toBuilder(), po);
		}
		
		if(saveFlag && accountWaitForEventCount==0 && positionWaitForEventCount==0) {
			saveFlag = false;
			for(String gatewayId : gatewayAccountUpdateSet) {
				Account po = accountMap.get(gatewayId);
				accountRepo.save(po);
			}
			gatewayAccountUpdateSet.clear();
		}
	}

}
