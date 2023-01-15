package tech.quantit.northstar.gateway.tiger;

import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.TradeGateway;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class TigerTradeGatewayAdapter implements TradeGateway{
	
	
	@Override
	public void connect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getAuthErrorFlag() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String submitOrder(SubmitOrderReqField submitOrderReq) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public GatewayDescription gatewayDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String gatewayId() {
		// TODO Auto-generated method stub
		return null;
	}

}
