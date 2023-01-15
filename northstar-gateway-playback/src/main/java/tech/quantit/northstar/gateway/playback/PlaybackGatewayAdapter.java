package tech.quantit.northstar.gateway.playback;

import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.MarketGateway;
import xyz.redtorch.pb.CoreField.ContractField;

public class PlaybackGatewayAdapter implements MarketGateway {
	
	private PlaybackContext ctx;
	
	private GatewayDescription gd;
	
	public PlaybackGatewayAdapter(PlaybackContext ctx, GatewayDescription gd) {
		this.ctx = ctx;
		this.gd = gd;
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
	public ChannelType channelType() {
		return ChannelType.PLAYBACK;
	}

	@Override
	public GatewayDescription gatewayDescription() {
		return gd;
	}

	@Override
	public String gatewayId() {
		return gd.getGatewayId();
	}

}
