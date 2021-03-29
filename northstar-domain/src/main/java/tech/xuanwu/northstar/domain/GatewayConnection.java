package tech.xuanwu.northstar.domain;

import com.google.common.eventbus.EventBus;

import tech.xuanwu.northstar.common.constant.GatewayConnectionState;
import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.model.GatewayDescription;

/**
 * 网关连接
 * @author KevinHuangwl
 *
 */
public abstract class GatewayConnection {
	
	protected GatewayDescription gwDescription;
	protected EventBus eventBus;
	
	protected GatewayConnectionState connectionState;
	protected boolean errorFlag;
	
	public GatewayConnection(GatewayDescription gwDescription, InternalEventBus eventBus) {
		this.gwDescription = gwDescription;
		this.eventBus = eventBus;
	}
	
	public void onConnected() {
		connectionState = GatewayConnectionState.CONNECTED;
		errorFlag = false;
	}
	
	public void onDisconnected() {
		connectionState = GatewayConnectionState.DISCONNECTED;
	}
	
	public boolean isConnected() {
		return connectionState == GatewayConnectionState.CONNECTED;
	}
	
	public void onError() {
		errorFlag = true;
	}
	
	public boolean hasConnectionError() {
		return errorFlag;
	}
	
	public void connect() {
		connectionState = GatewayConnectionState.CONNECTING;
		eventBus.post(new NorthstarEvent(NorthstarEventType.CONNECTING, gwDescription.getGatewayId()));
	}
	
	public void disconnect() {
		connectionState = GatewayConnectionState.DISCONNECTING;
		eventBus.post(new NorthstarEvent(NorthstarEventType.DISCONNECTING, gwDescription.getGatewayId()));
	}

	public GatewayDescription getGwDescription() {
		return gwDescription;
	}

	public void setGwDescription(GatewayDescription gwDescription) {
		this.gwDescription = gwDescription;
	}
	
}
