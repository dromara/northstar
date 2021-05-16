package tech.xuanwu.northstar.handler;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.gateway.api.MarketGateway;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import xyz.redtorch.pb.CoreField.ContractField;

public class ContractHandler extends AbstractEventHandler implements InternalEventHandler{

	private ContractManager contractMgr;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	public ContractHandler(ContractManager contractMgr, GatewayAndConnectionManager gatewayConnMgr) {
		this.contractMgr = contractMgr;
		this.gatewayConnMgr = gatewayConnMgr;
	}
	
	@Override
	public void doHandle(NorthstarEvent e) {
		ContractField contract = (ContractField) e.getData();
		String originalGatewayId = contract.getGatewayId();
		String relativeGatewayId = gatewayConnMgr.getGatewayConnectionById(originalGatewayId).getGwDescription().getRelativeGatewayId();
		
		ContractField contractNew = contract.toBuilder()
				.setGatewayId(relativeGatewayId)
				.build();
		if(contractMgr.addContract(contractNew)) {			
			MarketGateway gateway = (MarketGateway) gatewayConnMgr.getGatewayById(relativeGatewayId);
			if(gateway.isConnected()) {				
				gateway.subscribe(contractNew);
			}
		}
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.CONTRACT == eventType;
	}

}
