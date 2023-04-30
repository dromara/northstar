package org.dromara.northstar.gateway.playback.utils;

import java.time.LocalDateTime;

public interface PlaybackClock {

	/**
	 * 获取下一分钟的时间戳 
	 * @return
	 */
	LocalDateTime nextMarketMinute();
}
