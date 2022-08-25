package tech.quantit.northstar.data;

import tech.quantit.northstar.common.model.PlaybackRuntimeDescription;

public interface IPlaybackRuntimeRepository {
	
	/**
	 * 保存回放状态
	 * @param playbackRtDescription
	 */
	void save(PlaybackRuntimeDescription playbackRtDescription);
	
	/**
	 * 查询回放状态
	 * @param playbackGatewayId
	 * @return
	 */
	PlaybackRuntimeDescription findById(String playbackGatewayId);
	
	/**
	 * 移除回放状态
	 * @param playbackGatewayId
	 */
	void deleteById(String playbackGatewayId);
}
