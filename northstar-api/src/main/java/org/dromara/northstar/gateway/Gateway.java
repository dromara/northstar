package org.dromara.northstar.gateway;

import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.model.GatewayDescription;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Gateway {

	/* 连接状态 */
	protected ConnectionState connection = ConnectionState.DISCONNECTED;
	
	/* 网关信息 */
	protected GatewayDescription description;
	
	protected Gateway(GatewayDescription description) {
		this.description = description;
	}

	/**
	 * 连接
	 */
	public abstract void connect();
	
	/**
	 * 断开
	 */
	public abstract void disconnect();
}
