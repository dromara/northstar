package tech.xuanwu.northstar.factories;

import com.google.common.collect.Table;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.domain.TradeDayAccount;
import xyz.redtorch.pb.CoreField.ContractField;

public class TradeDayAccountFactory {
	
	private Table<String, String, ContractField> contractTable;
	private InternalEventBus eventBus;
	
	public TradeDayAccountFactory(InternalEventBus eventBus, Table<String, String, ContractField> contractTable) {
		this.eventBus = eventBus;
		this.contractTable = contractTable;
	}
	
	public TradeDayAccount newInstance(String gatewayId) {
		return new TradeDayAccount(gatewayId, eventBus, contractTable.row(gatewayId));
	}

}
