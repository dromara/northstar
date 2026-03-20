package org.dromara.northstar.event;

import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.NoSuchGatewayException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.MarketGateway;

import lombok.extern.slf4j.Slf4j;

/**
 * 处理连接相关操作
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ConnectionHandler extends AbstractEventHandler implements GenericEventHandler{
	
	protected GatewayManager gatewayMgr;
	protected IContractManager contractMgr;
	protected IGatewayRepository gatewayRepo;
	
	public ConnectionHandler(GatewayManager gatewayMgr, IContractManager contractMgr, IGatewayRepository gatewayRepo) {
		this.gatewayMgr = gatewayMgr;
		this.contractMgr = contractMgr;
		this.gatewayRepo = gatewayRepo;
	}

	@Override
	public void doHandle(NorthstarEvent e) {
		String gatewayId = (String) e.getData();
		Identifier id = Identifier.of(gatewayId);
		if(!gatewayMgr.contains(id)) {
			throw new NoSuchGatewayException(gatewayId);
		}
		log.info("[{}]-已可用", gatewayId);
		Gateway gateway = gatewayMgr.get(id);
		if(gateway instanceof MarketGateway mktGateway) {
			mktGateway.getDescription().getSubscribedContracts()
				.forEach(csi -> contractMgr.getContract(Identifier.of(csi.getId())).subscribe());
		}
	}
	

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.GATEWAY_READY;
	}

}
