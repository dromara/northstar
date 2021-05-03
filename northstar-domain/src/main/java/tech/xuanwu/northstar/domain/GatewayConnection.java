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
	
	protected boolean errorFlag;
	
	public GatewayConnection(GatewayDescription gwDescription, InternalEventBus eventBus) {
		this.gwDescription = gwDescription;
		this.eventBus = eventBus;
	}
	
	public void onConnected() {
		gwDescription.setConnectionState(GatewayConnectionState.CONNECTED);
		errorFlag = false;
	}
	
	public void onDisconnected() {
		gwDescription.setConnectionState(GatewayConnectionState.DISCONNECTED);
	}
	
	public void onConnecting() {
		gwDescription.setConnectionState(GatewayConnectionState.CONNECTING);
	}
	
	public void onDisconnecting() {
		gwDescription.setConnectionState(GatewayConnectionState.DISCONNECTING);
	}
	
	public boolean isConnected() {
		return GatewayConnectionState.CONNECTED.equals(gwDescription.getConnectionState());
	}
	
	public void onError() {
		errorFlag = true;
	}
	
	public boolean hasConnectionError() {
		return errorFlag;
	}
	
	public void connect() {
		gwDescription.setConnectionState(GatewayConnectionState.CONNECTING);
		eventBus.post(new NorthstarEvent(NorthstarEventType.CONNECTING, gwDescription.getGatewayId()));
	}
	
	public void disconnect() {
		gwDescription.setConnectionState(GatewayConnectionState.DISCONNECTING);
		eventBus.post(new NorthstarEvent(NorthstarEventType.DISCONNECTING, gwDescription.getGatewayId()));
	}

	public GatewayDescription getGwDescription() {
		return gwDescription;
	}

	public void setGwDescription(GatewayDescription gwDescription) {
		this.gwDescription = gwDescription;
	}
	
}
