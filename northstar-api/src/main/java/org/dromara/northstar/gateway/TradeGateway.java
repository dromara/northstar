package org.dromara.northstar.gateway;

import org.dromara.northstar.common.model.GatewayDescription;
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
	public abstract String submitOrder(SubmitOrderReq submitOrderReq);

	/**
	 * 撤销定单
	 * 
	 * @param cancelOrderReq
	 * @return
	 */
	public abstract boolean cancelOrder(String originOrderId);
	
	/**
	 * 交易账户出入金
	 * @return 返回出入金后账户余额
	 */
	public int moneyIO(int amount) {
		throw new UnsupportedOperationException();
	}
	
}
