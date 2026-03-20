package org.dromara.northstar.gateway.playback;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.core.Bar;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
public class GlobalPlaybackDataDetector implements BarDataAware {
	
	@Getter
	private long lastEventTime;

	@Override
	public void onBar(Bar bar) {
		if(bar.channelType() != ChannelType.PLAYBACK) {
			return;
		}
		lastEventTime = System.currentTimeMillis();
	}

}
