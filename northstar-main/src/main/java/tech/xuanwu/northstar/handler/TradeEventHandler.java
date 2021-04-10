package tech.xuanwu.northstar.handler;

import java.util.Map;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.gateway.api.TradeGateway;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class TradeEventHandler extends AbstractEventHandler implements InternalEventHandler{
	
	protected Map<String, GatewayConnection> traderGatewayMap;
	protected Map<GatewayConnection, Gateway> gatewayMap;
	
	public TradeEventHandler(Map<String, GatewayConnection> traderGatewayMap, Map<GatewayConnection, Gateway> gatewayMap) {
		this.traderGatewayMap = traderGatewayMap;
		this.gatewayMap = gatewayMap;
	}

	@Override
	public void doHandle(NorthstarEvent e) {
		if (e.getEvent() == NorthstarEventType.PLACE_ORDER) {
			SubmitOrderReqField submitReq = (SubmitOrderReqField) e.getData();
			String gatewayId = submitReq.getGatewayId();
			if(!traderGatewayMap.containsKey(gatewayId)) {
				throw new NoSuchElementException("没有找到相关的网关：" + gatewayId);
			}
			TradeGateway tradeGateway = (TradeGateway) gatewayMap.get(traderGatewayMap.get(gatewayId));
			tradeGateway.submitOrder(submitReq);
			
		} else if (e.getEvent() == NorthstarEventType.WITHDRAW_ORDER) {
			CancelOrderReqField cancelReq = (CancelOrderReqField) e.getData();
			String gatewayId = cancelReq.getGatewayId();
			if(!traderGatewayMap.containsKey(gatewayId)) {
				throw new NoSuchElementException("没有找到相关的网关：" + gatewayId);
			}
			TradeGateway tradeGateway = (TradeGateway) gatewayMap.get(traderGatewayMap.get(gatewayId));
			tradeGateway.cancelOrder(cancelReq);
		}
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventType == NorthstarEventType.PLACE_ORDER || eventType == NorthstarEventType.WITHDRAW_ORDER;
	}

}
