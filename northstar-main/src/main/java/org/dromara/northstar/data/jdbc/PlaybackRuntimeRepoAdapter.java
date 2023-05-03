package org.dromara.northstar.data.jdbc;

import java.util.Objects;

import org.dromara.northstar.common.model.PlaybackRuntimeDescription;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.data.jdbc.entity.PlaybackRuntimeDescriptionDO;

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
		PlaybackRuntimeDescriptionDO obj = delegate.findById(playbackGatewayId).orElse(null);
		if(Objects.isNull(obj)) {
			return null;
		}
		return obj.convertTo();
	}

	@Override
	public void deleteById(String playbackGatewayId) {
		if(delegate.existsById(playbackGatewayId)) {
			delegate.deleteById(playbackGatewayId);
		}
	}

}
