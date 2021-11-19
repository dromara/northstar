package tech.xuanwu.northstar.gateway.api;

import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public interface TradeGateway extends Gateway {

	/**
	 * 提交定单
	 * 
	 * @param orderReq
	 * @return
	 */
	String submitOrder(SubmitOrderReqField submitOrderReq);

	/**
	 * 撤销定单
	 * 
	 * @param cancelOrderReq
	 * @return
	 */
	boolean cancelOrder(CancelOrderReqField cancelOrderReq);
	
}
