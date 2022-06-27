package tech.quantit.northstar.gateway.playback;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.gateway.api.MarketGateway;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public class PlaybackGatewayAdapter implements MarketGateway{

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
		// TODO Auto-generated method stub
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
	public GatewayType gatewayType() {
		// TODO Auto-generated method stub
		return null;
	}

}
