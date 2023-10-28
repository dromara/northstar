package org.dromara.northstar.gateway.playback.utils;

import java.time.LocalDateTime;

@Deprecated(forRemoval = true)
public interface PlaybackClock {

	/**
	 * 获取下一分钟的时间戳 
	 * @return
	 */
	LocalDateTime nextMarketMinute();
}
