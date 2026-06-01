package org.dromara.northstar.gateway.playback;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.mktdata.MarketCenter;

import lombok.Getter;

public class PlaybackGatewayAdapter extends MarketGateway {
	
	@Getter
	private PlaybackContext context;
	
	public PlaybackGatewayAdapter(GatewayDescription gatewayDescription, FastEventEngine feEngine, MarketCenter mktCenter, 
			 GlobalPlaybackDataDetector dataDetector) {
		super(gatewayDescription);
		this.context = new PlaybackContext(this, feEngine, mktCenter, dataDetector);
	}

	@Override
	public void connect() {
		context.start();
	}

	@Override
	public void disconnect() {
		context.pause();
	}
	
	@Override
	public boolean subscribe() {
		// 动态订阅不需要实现
		return true;
	}

	@Override
	public boolean unsubscribe() {
		// 动态取消订阅不需要实现
		return true;
	}

	@Override
	public boolean isActive() {
		return context.getPlaybackStatus().getState() == PlaybackStatus.State.PLAYING
				|| context.getPlaybackStatus().getState() == PlaybackStatus.State.PRELOADING;
	}

	@Override
	public void setDescription(GatewayDescription description) {
		super.setDescription(description);
		context.init(); // 配置修复后，要重新初始化
	}
	
}
