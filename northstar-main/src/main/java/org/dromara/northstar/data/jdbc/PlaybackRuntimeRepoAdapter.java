package org.dromara.northstar.data.jdbc;

import org.dromara.northstar.common.model.PlaybackRuntimeDescription;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.data.jdbc.model.PlaybackRuntimeDescriptionDO;

public class PlaybackRuntimeRepoAdapter implements IPlaybackRuntimeRepository{

	private PlaybackRuntimeRepository delegate;
	
	public PlaybackRuntimeRepoAdapter(PlaybackRuntimeRepository delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public void save(PlaybackRuntimeDescription playbackRtDescription) {
		delegate.save(PlaybackRuntimeDescriptionDO.convertFrom(playbackRtDescription));
	}

	@Override
	public PlaybackRuntimeDescription findById(String playbackGatewayId) {
		return delegate.findById(playbackGatewayId).orElseThrow().convertTo();
	}

	@Override
	public void deleteById(String playbackGatewayId) {
		delegate.deleteById(playbackGatewayId);
	}

}
