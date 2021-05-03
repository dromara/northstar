package tech.xuanwu.northstar.handler;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.gateway.api.TradeGateway;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class TradeEventHandler extends AbstractEventHandler implements InternalEventHandler{
	
	protected GatewayAndConnectionManager gatewayConnMgr;
	
	public TradeEventHandler(GatewayAndConnectionManager gatewayConnMgr) {
		this.gatewayConnMgr = gatewayConnMgr;
	}

	@Override
	public void doHandle(NorthstarEvent e) {
		if (e.getEvent() == NorthstarEventType.PLACE_ORDER) {
			SubmitOrderReqField submitReq = (SubmitOrderReqField) e.getData();
			String gatewayId = submitReq.getGatewayId();
			if(!gatewayConnMgr.exist(gatewayId)) {
				throw new NoSuchElementException("没有找到相关的网关：" + gatewayId);
			}
			TradeGateway tradeGateway = (TradeGateway) gatewayConnMgr.getGatewayById(gatewayId);
			tradeGateway.submitOrder(submitReq);
			
		} else if (e.getEvent() == NorthstarEventType.WITHDRAW_ORDER) {
			CancelOrderReqField cancelReq = (CancelOrderReqField) e.getData();
			String gatewayId = cancelReq.getGatewayId();
			if(!gatewayConnMgr.exist(gatewayId)) {
				throw new NoSuchElementException("没有找到相关的网关：" + gatewayId);
			}
			TradeGateway tradeGateway = (TradeGateway) gatewayConnMgr.getGatewayById(gatewayId);
			tradeGateway.cancelOrder(cancelReq);
		}
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.PLACE_ORDER || eventType == NorthstarEventType.WITHDRAW_ORDER;
	}

}
