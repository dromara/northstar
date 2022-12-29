package tech.quantit.northstar.main.handler.internal;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.Subscribable;
import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.GenericEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.ContractSimpleInfo;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.gateway.GatewayConnection;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.MarketGateway;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;

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
			if(gateway instanceof MarketGateway mktGateway && gateway.getGatewaySetting().getGatewayType() == GatewayTypeEnum.GTE_MarketData) {
				doSubscribe(mktGateway.getGatewaySetting().getGatewayId());
			}
		}
	}
	
	private void doSubscribe(String gatewayId) {
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		if(!gd.getSubscribedContracts().isEmpty()) {					
			for(ContractSimpleInfo contractInfo : gd.getSubscribedContracts()) {
				Subscribable obj = contractMgr.getContract(Identifier.of(contractInfo.getIdentifier()));
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
