package tech.xuanwu.northstar.handler;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import xyz.redtorch.pb.CoreField.TickField;


public class IndexContractHandler extends AbstractEventHandler implements InternalEventHandler{
	
	private ContractManager contractMgr;
	
	
	public IndexContractHandler(ContractManager contractMgr, FastEventEngine fastEventEngine, SocketIOMessageEngine msgEngine) {
		this.contractMgr = contractMgr;
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.TICK == eventType || NorthstarEventType.CONTRACT_LOADED == eventType;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	private void generateIndexContract(String gatewayId) {
		
	}
	
	private void handleIndexContractUpdate(TickField tick) {}

}
