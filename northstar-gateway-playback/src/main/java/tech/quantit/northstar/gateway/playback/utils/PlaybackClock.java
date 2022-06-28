package tech.quantit.northstar.gateway.playback.utils;

public interface PlaybackClock {

	/**
	 * 获取下一分钟的时间戳 
	 * @return
	 */
	long nextMarketMinute();
}
