package tech.quantit.northstar.gateway.tiger;

import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.MarketGateway;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public class TigerGatewayAdapter implements MarketGateway {

	private GatewayDescription gd;
	
	private FastEventEngine feEngine;
	
	public TigerGatewayAdapter(GatewayDescription gd, FastEventEngine feEngine) {
		this.gd = gd;
		this.feEngine = feEngine;
	}
	
	@Override
	public GatewaySettingField getGatewaySetting() {
		return GatewaySettingField.newBuilder()
				.setGatewayId(gd.getGatewayId())
				.setGatewayType(GatewayTypeEnum.GTE_MarketData)
				.build();
	}

	@Override
	public void connect() {
		
	}

	@Override
	public void disconnect() {
		
	}

	@Override
	public boolean isConnected() {
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
		return false;
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.TIGER;
	}

}
