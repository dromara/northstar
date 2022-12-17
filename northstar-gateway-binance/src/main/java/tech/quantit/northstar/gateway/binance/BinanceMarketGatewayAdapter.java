package tech.quantit.northstar.gateway.binance;

import tech.quantit.northstar.gateway.api.MarketGateway;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public class BinanceMarketGatewayAdapter implements MarketGateway {

	@Override
	public GatewaySettingField getGatewaySetting() {
		// TODO Auto-generated method stub
		return null;
	}

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
	public String gatewayType() {
		// TODO Auto-generated method stub
		return null;
	}

}
