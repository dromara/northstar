package tech.xuanwu.northstar.handler;

import tech.xuanwu.northstar.common.constant.GatewayConnectionState;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;

public class ConnectionEventHandler extends AbstractEventHandler implements InternalEventHandler{
	
	protected GatewayAndConnectionManager gatewayConnMgr;
	
	public ConnectionEventHandler(GatewayAndConnectionManager gatewayConnMgr) {
		this.gatewayConnMgr = gatewayConnMgr;
	}

	@Override
	public void doHandle(NorthstarEvent e) {
		String gatewayId = (String) e.getData();
		
		
		if(!gatewayConnMgr.exist(gatewayId)) {
			throw new NoSuchElementException("没有找到相关的网关：" + gatewayId);
		}
		Gateway gateway = gatewayConnMgr.getGatewayById(gatewayId);
		GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		if(e.getEvent() == NorthstarEventType.CONNECTING) {
			if(conn.getGwDescription().getConnectionState() != GatewayConnectionState.CONNECTING) {				
				gateway.connect();
			}
			conn.onConnecting();
		} else if(e.getEvent() == NorthstarEventType.DISCONNECTING) {
			if(conn.getGwDescription().getConnectionState() != GatewayConnectionState.DISCONNECTING) {				
				gateway.disconnect();
			}
			conn.onDisconnecting();
		} else if(e.getEvent() == NorthstarEventType.CONNECTED) {
			conn.onConnected();
		} else if(e.getEvent() == NorthstarEventType.DISCONNECTED) {
			conn.onDisconnected();
		}
		
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.CONNECTING || eventType == NorthstarEventType.DISCONNECTING 
				|| eventType == NorthstarEventType.CONNECTED || eventType == NorthstarEventType.DISCONNECTED;
	}

}
