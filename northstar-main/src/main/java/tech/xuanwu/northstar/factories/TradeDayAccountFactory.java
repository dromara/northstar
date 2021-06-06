package tech.xuanwu.northstar.factories;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.domain.ContractManager;
import tech.xuanwu.northstar.domain.account.TradeDayAccount;

public class TradeDayAccountFactory {
	
	private ContractManager contractMgr;
	private InternalEventBus eventBus;
	
	public TradeDayAccountFactory(InternalEventBus eventBus, ContractManager contractMgr) {
		this.eventBus = eventBus;
		this.contractMgr = contractMgr;
	}
	
	public TradeDayAccount newInstance(String gatewayId) {
		return new TradeDayAccount(gatewayId, eventBus, contractMgr);
	}

}
