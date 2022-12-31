package tech.quantit.northstar.gateway.playback;

import tech.quantit.northstar.gateway.api.MarketGateway;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public class PlaybackGatewayAdapter implements MarketGateway {
	
	private PlaybackContext ctx;
	
	private GatewaySettingField settings;
	
	public PlaybackGatewayAdapter(PlaybackContext ctx, GatewaySettingField settings) {
		this.ctx = ctx;
		this.ctx.setGatewaySettings(settings);
		this.settings = settings;
	}

	@Override
	public GatewaySettingField getGatewaySetting() {
		return settings;
	}

	@Override
	public void connect() {
		ctx.start();
	}

	@Override
	public void disconnect() {
		ctx.stop();
	}

	@Override
	public boolean isConnected() {
		return ctx.isRunning();
	}

	@Override
	public boolean getAuthErrorFlag() {
		return false;
	}

	@Override
	public boolean subscribe(ContractField contract) {
		// 动态订阅不需要实现
		return true;
	}

	@Override
	public boolean unsubscribe(ContractField contract) {
		// 动态取消订阅不需要实现
		return true;
	}

	@Override
	public boolean isActive() {
		return ctx.isRunning();
	}

	@Override
	public String channelType() {
		return PLAYBACK.class.getName();
	}

}
