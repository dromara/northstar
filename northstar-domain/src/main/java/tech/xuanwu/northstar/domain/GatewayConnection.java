package tech.xuanwu.northstar.domain;

import com.google.common.eventbus.EventBus;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.model.GatewayDescription;

/**
 * 网关连接
 * @author KevinHuangwl
 *
 */
public class GatewayConnection {
	
	protected GatewayDescription gwDescription;
	protected EventBus eventBus;
	
	protected int connectionState;
	protected boolean errorFlag;
	
	protected final int DISCONNECTED = 0;
	protected final int CONNECTING = 1;
	protected final int DISCONNECTING = 2;
	protected final int CONNECTED = 3;
	
	public GatewayConnection(GatewayDescription gwDescription, InternalEventBus eventBus) {
		this.gwDescription = gwDescription;
		this.eventBus = eventBus;
	}
	
	public void onConnected() {
		connectionState = CONNECTED;
		errorFlag = false;
	}
	
	public void onDisconnected() {
		connectionState = DISCONNECTED;
	}
	
	public boolean isConnected() {
		return connectionState == CONNECTED;
	}
	
	public void onError() {
		errorFlag = true;
	}
	
	public boolean hasConnectionError() {
		return errorFlag;
	}
	
	public void connect() {
		connectionState = CONNECTING;
		eventBus.post(gwDescription);
	}
	
	public void disconnect() {
		connectionState = DISCONNECTING;
	}
	
	
}
