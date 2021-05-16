package tech.xuanwu.northstar.handler;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.domain.ContractManager;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.domain.IndexContract;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import xyz.redtorch.pb.CoreField.TickField;


public class IndexContractHandler extends AbstractEventHandler implements InternalEventHandler{
	
	private ContractManager contractMgr;
	
	private FastEventEngine feEngine;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private SocketIOMessageEngine msgEngine;
	
	private Table<String, String, IndexContract> idxContractTbl = HashBasedTable.create();
	
	public IndexContractHandler(GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr,
			FastEventEngine fastEventEngine, SocketIOMessageEngine msgEngine) {
		this.contractMgr = contractMgr;
		this.feEngine = fastEventEngine;
		this.msgEngine = msgEngine;
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.TICK == eventType || NorthstarEventType.CONTRACT_LOADED == eventType;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(NorthstarEventType.TICK == e.getEvent()) {
			TickField tick = (TickField) e.getData();
			handleIndexContractUpdate(tick);
		} else if(NorthstarEventType.CONTRACT_LOADED == e.getEvent()) {
			String accGatewayId = (String) e.getData();
			GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(accGatewayId);
			String mktGatewayId = conn.getGwDescription().getRelativeGatewayId();
			generateIndexContract(mktGatewayId);
		}
	}
	
	private void generateIndexContract(String gatewayId) {
		
	}
	
	private void handleIndexContractUpdate(TickField tick) {}

}
