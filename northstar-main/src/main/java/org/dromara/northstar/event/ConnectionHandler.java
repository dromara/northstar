package org.dromara.northstar.event;

import java.util.HashSet;
import java.util.Set;

import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.Subscribable;
import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
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
	protected Set<String> subscribedSet = new HashSet<>();
	
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
			throw new NoSuchElementException("没有找到相关的网关：" + gatewayId);
		}
		log.info("[{}]-已可用", gatewayId);
		Gateway gateway = gatewayMgr.get(id);
		if(gateway instanceof MarketGateway mktGateway) {
			doSubscribe(mktGateway.gatewayId());
		}
	}
	
	private void doSubscribe(String gatewayId) {
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		if(!gd.getSubscribedContracts().isEmpty()) {					
			for(ContractSimpleInfo contractInfo : gd.getSubscribedContracts()) {
				Subscribable obj = contractMgr.getContract(Identifier.of(contractInfo.getValue()));
				obj.subscribe();
			}
		} 
		subscribedSet.add(gatewayId);
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.GATEWAY_READY;
	}

}
