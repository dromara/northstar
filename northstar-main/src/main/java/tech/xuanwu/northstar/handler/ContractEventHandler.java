package tech.xuanwu.northstar.handler;

import org.apache.commons.lang3.StringUtils;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.model.ContractManager;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import xyz.redtorch.pb.CoreField.ContractField;

public class ContractEventHandler extends AbstractEventHandler implements InternalEventHandler{

	private ContractManager contractMgr;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	public ContractEventHandler(ContractManager contractMgr, GatewayAndConnectionManager gatewayConnMgr) {
		this.contractMgr = contractMgr;
		this.gatewayConnMgr = gatewayConnMgr;
	}
	
	@Override
	public void doHandle(NorthstarEvent e) {
		ContractField contract = (ContractField) e.getData();
		String originalGatewayId = contract.getGatewayId();
		String relativeGatewayId = gatewayConnMgr.getGatewayConnectionById(originalGatewayId).getGwDescription().getRelativeGatewayId();
		
		contractMgr.addContract(contract.toBuilder()
				.setGatewayId(StringUtils.isNotBlank(relativeGatewayId) ? relativeGatewayId : originalGatewayId)
				.build()
			);
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.CONTRACT;
	}

}
