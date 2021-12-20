package tech.quantit.northstar.main.handler.internal;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.GenericEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.domain.account.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.account.GatewayConnection;
import tech.quantit.northstar.domain.gateway.ContractManager;

/**
 * 处理连接相关操作
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ConnectionHandler extends AbstractEventHandler implements GenericEventHandler{
	
	protected GatewayAndConnectionManager gatewayConnMgr;
	protected ContractManager contractMgr;
	
	private static final Set<NorthstarEventType> TARGET_TYPE = new HashSet<>() {
		private static final long serialVersionUID = 6418831877479036414L;
		{
			add(NorthstarEventType.CONNECTING);
			add(NorthstarEventType.CONNECTED);
			add(NorthstarEventType.DISCONNECTED);
			add(NorthstarEventType.DISCONNECTING);
		}
	};
	
	public ConnectionHandler(GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
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
			conn.onDisconnecting();
		} else if(e.getEvent() == NorthstarEventType.CONNECTED) {
			log.info("[{}]-已连接", gatewayId);
			conn.onConnected();
		} else if(e.getEvent() == NorthstarEventType.DISCONNECTED) {
			log.info("[{}]-已断开", gatewayId);
			conn.onDisconnected();
		}
		
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}

}
