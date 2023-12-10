package org.dromara.northstar.gateway;

import org.dromara.northstar.common.model.core.SubmitOrderReq;

public interface TradeGateway extends Gateway {

	/**
	 * 提交定单
	 * 
	 * @param orderReq
	 * @return
	 */
	String submitOrder(SubmitOrderReq submitOrderReq);

	/**
	 * 撤销定单
	 * 
	 * @param cancelOrderReq
	 * @return
	 */
	boolean cancelOrder(String originOrderId);
	
}
