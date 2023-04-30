package org.dromara.northstar.gateway.binance;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.MarketGateway;

import xyz.redtorch.pb.CoreField.ContractField;

public class BinanceMarketGatewayAdapter implements MarketGateway {

	private FastEventEngine feEngine;
	
	private GatewayDescription gd;
	
	public BinanceMarketGatewayAdapter(GatewayDescription gd, FastEventEngine feEngine) {
		this.gd = gd;
		this.feEngine = feEngine;
	}
	
	@Override
	public void connect() {
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getAuthErrorFlag() {
		return false;
	}

	@Override
	public boolean subscribe(ContractField contract) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unsubscribe(ContractField contract) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ChannelType channelType() {
		return null;
	}

	@Override
	public GatewayDescription gatewayDescription() {
		gd.setConnectionState(getConnectionState());
		return gd;
	}

	@Override
	public String gatewayId() {
		return gd.getGatewayId();
	}

	@Override
	public ConnectionState getConnectionState() {
		// TODO Auto-generated method stub
		return null;
	}

}
