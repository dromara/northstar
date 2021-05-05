package tech.xuanwu.northstar.domain;

import com.google.common.eventbus.EventBus;

import tech.xuanwu.northstar.common.constant.ConnectionState;
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
	
	protected boolean errorFlag;
	
	public GatewayConnection(GatewayDescription gwDescription, InternalEventBus eventBus) {
		this.gwDescription = gwDescription;
		this.eventBus = eventBus;
	}
	
	public void onConnected() {
		gwDescription.setConnectionState(ConnectionState.CONNECTED);
		errorFlag = false;
	}
	
	public void onDisconnected() {
		gwDescription.setConnectionState(ConnectionState.DISCONNECTED);
	}
	
	public void onConnecting() {
		gwDescription.setConnectionState(ConnectionState.CONNECTING);
	}
	
	public void onDisconnecting() {
		gwDescription.setConnectionState(ConnectionState.DISCONNECTING);
	}
	
	public boolean isConnected() {
		return ConnectionState.CONNECTED.equals(gwDescription.getConnectionState());
	}
	
	public void onError() {
		errorFlag = true;
	}
	
	public boolean hasConnectionError() {
		return errorFlag;
	}
	
	public void connect() {
		gwDescription.setConnectionState(ConnectionState.CONNECTING);
		eventBus.post(new NorthstarEvent(NorthstarEventType.CONNECTING, gwDescription.getGatewayId()));
	}
	
	public void disconnect() {
		gwDescription.setConnectionState(ConnectionState.DISCONNECTING);
		eventBus.post(new NorthstarEvent(NorthstarEventType.DISCONNECTING, gwDescription.getGatewayId()));
	}

	public GatewayDescription getGwDescription() {
		return gwDescription;
	}

	public void setGwDescription(GatewayDescription gwDescription) {
		this.gwDescription = gwDescription;
	}
	
}
