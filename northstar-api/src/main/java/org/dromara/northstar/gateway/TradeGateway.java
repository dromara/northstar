package org.dromara.northstar.gateway;

import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.SubmitOrderReq;

public abstract class TradeGateway extends Gateway {

	protected TradeGateway(GatewayDescription description) {
		super(description);
	}

	/**
	 * 提交定单
	 * 
	 * @param orderReq
	 * @return
	 */
	public abstract void submitOrder(SubmitOrderReq submitOrderReq);

	/**
	 * 撤销定单
	 * 
	 * @param cancelOrderReq
	 * @return
	 */
	public abstract boolean cancelOrder(CancelOrderReq cancelOrderReq);
	
}
