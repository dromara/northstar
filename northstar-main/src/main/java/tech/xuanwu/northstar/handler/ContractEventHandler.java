package tech.xuanwu.northstar.handler;

import com.google.common.collect.Table;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import xyz.redtorch.pb.CoreField.ContractField;

public class ContractEventHandler extends AbstractEventHandler implements InternalEventHandler{

	private Table<String, String, ContractField> gatewayContractTable;
	
	public ContractEventHandler(Table<String, String, ContractField> gatewayContractTable) {
		this.gatewayContractTable = gatewayContractTable;
	}
	
	@Override
	public void doHandle(NorthstarEvent e) {
		ContractField contract = (ContractField) e.getData();
		String gatewayId = contract.getGatewayId();
		String symbol = contract.getSymbol();
		gatewayContractTable.put(gatewayId, symbol, contract);
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.CONTRACT;
	}

}
