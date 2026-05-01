package org.dromara.northstar.gateway;

import org.dromara.northstar.common.model.GatewayDescription;

public abstract class MarketGateway extends Gateway {
	
	protected MarketGateway(GatewayDescription description) {
		super(description);
	}

	/**
	 * 检测是否有行情数据
	 * @return
	 */
	public abstract boolean isActive();

	/**
	 * 订阅
	 * @param subscribeReq
	 */
	public abstract boolean subscribe();

	/**
	 * 退订
	 * @param subscribeReq
	 */
	public abstract boolean unsubscribe();
	
}
