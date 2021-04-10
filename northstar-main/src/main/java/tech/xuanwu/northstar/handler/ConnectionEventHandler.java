package tech.xuanwu.northstar.handler;

import java.util.Map;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.gateway.api.Gateway;

public class ConnectionEventHandler extends AbstractEventHandler implements InternalEventHandler{
	
	protected Map<GatewayConnection, Gateway> gatewayMap;
	
	public ConnectionEventHandler(Map<GatewayConnection, Gateway> gatewayMap) {
		this.gatewayMap = gatewayMap;
	}

	@Override
	public void doHandle(NorthstarEvent e) {
		GatewayConnection conn = (GatewayConnection) e.getData();
		if(!gatewayMap.containsKey(conn)) {
			throw new NoSuchElementException("没有找到相关的网关：" + conn.getGwDescription().getGatewayId());
		}
		Gateway gateway = gatewayMap.get(conn);
		if(e.getEvent() == NorthstarEventType.CONNECTING) {
			gateway.connect();
		} else if(e.getEvent() == NorthstarEventType.DISCONNECTING) {
			gateway.disconnect();
		}
		
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.CONNECTING || eventType == NorthstarEventType.DISCONNECTING;
	}

}
