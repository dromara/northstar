package org.dromara.northstar.event;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.dromara.northstar.account.GatewayAndConnectionManager;
import org.dromara.northstar.account.GatewayConnection;
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
import org.dromara.northstar.gateway.api.Gateway;
import org.dromara.northstar.gateway.api.IContractManager;
import org.dromara.northstar.gateway.api.MarketGateway;

import lombok.extern.slf4j.Slf4j;

/**
 * 处理连接相关操作
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ConnectionHandler extends AbstractEventHandler implements GenericEventHandler{
	
	protected GatewayAndConnectionManager gatewayConnMgr;
	protected IContractManager contractMgr;
	protected IGatewayRepository gatewayRepo;
	protected Set<String> subscribedSet = new HashSet<>();
	
	private static final Set<NorthstarEventType> TARGET_TYPE = EnumSet.of(
			NorthstarEventType.CONNECTING,
			NorthstarEventType.CONNECTED,
			NorthstarEventType.DISCONNECTED,
			NorthstarEventType.DISCONNECTING,
			NorthstarEventType.GATEWAY_READY
	); 
	
	public ConnectionHandler(GatewayAndConnectionManager gatewayConnMgr, IContractManager contractMgr, IGatewayRepository gatewayRepo) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
		this.gatewayRepo = gatewayRepo;
	}

	@Override
	public void doHandle(NorthstarEvent e) {
		String gatewayId = (String) e.getData();
		if(!gatewayConnMgr.exist(gatewayId)) {
			throw new NoSuchElementException("没有找到相关的网关：" + gatewayId);
		}
		GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		if(e.getEvent() == NorthstarEventType.CONNECTING) {
			log.info("[{}]-连接中", gatewayId);
			conn.onConnecting();
		} else if(e.getEvent() == NorthstarEventType.DISCONNECTING) {
			log.info("[{}]-断开中", gatewayId);
			subscribedSet.remove(gatewayId);
			conn.onDisconnecting();
		} else if(e.getEvent() == NorthstarEventType.CONNECTED) {
			log.info("[{}]-已连接", gatewayId);
			conn.onConnected();
		} else if(e.getEvent() == NorthstarEventType.DISCONNECTED) {
			log.info("[{}]-已断开", gatewayId);
			subscribedSet.remove(gatewayId);
			conn.onDisconnected();
		} else if(e.getEvent() == NorthstarEventType.GATEWAY_READY) {
			log.info("[{}]-已可用", gatewayId);
			Gateway gateway = gatewayConnMgr.getGatewayById(gatewayId);
			if(gateway instanceof MarketGateway mktGateway) {
				doSubscribe(mktGateway.gatewayId());
			}
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
		return TARGET_TYPE.contains(eventType);
	}

}
