package tech.quantit.northstar.main.factories;

import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.ContractManager;

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
