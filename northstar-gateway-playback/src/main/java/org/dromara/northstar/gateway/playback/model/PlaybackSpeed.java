package org.dromara.northstar.gateway.playback.model;

/**
 * 回放速度
 * @author KevinHuangwl
 *
 */
public enum PlaybackSpeed {
	/**
	 * 正常速度
	 */
	NORMAL(500),
	/**
	 * 快速
	 */
	SPRINT(10),
	/**
	 * 极速
	 */
	RUSH(0);
	
	private int pauseInterval;
	private PlaybackSpeed(int pauseInterval) {
		this.pauseInterval = pauseInterval;
	}
	
	public int getPauseInterval() {
		return pauseInterval;
	}
}
