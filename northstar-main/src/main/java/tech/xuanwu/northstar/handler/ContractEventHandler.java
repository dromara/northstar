package tech.xuanwu.northstar.handler;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.model.ContractManager;
import xyz.redtorch.pb.CoreField.ContractField;

public class ContractEventHandler extends AbstractEventHandler implements InternalEventHandler{

	private ContractManager contractMgr;
	
	public ContractEventHandler(ContractManager contractMgr) {
		this.contractMgr = contractMgr;
	}
	
	@Override
	public void doHandle(NorthstarEvent e) {
		ContractField contract = (ContractField) e.getData();
		contractMgr.addContract(contract);
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.CONTRACT;
	}

}
