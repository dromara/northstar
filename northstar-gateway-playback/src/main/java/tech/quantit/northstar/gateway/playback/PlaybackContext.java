package tech.quantit.northstar.gateway.playback;

import tech.quantit.northstar.common.constant.PlaybackSpeed;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.gateway.playback.ticker.TickSimulationAlgorithm;

public class PlaybackContext {
	
	private IPlaybackRuntimeRepository rtRepo;
	
	private FastEventEngine feEngine;
	
	private PlaybackDataLoader loader;
	
	private TickSimulationAlgorithm tickerAlgo;
	
	// 回放速度
	private PlaybackSpeed playbackSpeed;
	
	// 回放时间戳状态
	private long playbackTimeState;
	
	/**
	 * 开始回放
	 */
	public void start() {}
	
	/**
	 * 暂停回放
	 */
	public void stop() {}
}
