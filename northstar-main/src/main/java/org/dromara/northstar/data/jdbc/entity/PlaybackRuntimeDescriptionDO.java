package org.dromara.northstar.data.jdbc.entity;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.dromara.northstar.common.model.PlaybackRuntimeDescription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="PLAYBACK_RT")
public class PlaybackRuntimeDescriptionDO {

	@Id
	private String gatewayId;
	
	private LocalDateTime playbackTimeState;
	
	public static PlaybackRuntimeDescriptionDO convertFrom(PlaybackRuntimeDescription playbackRtDescription) {
		return new PlaybackRuntimeDescriptionDO(playbackRtDescription.getGatewayId(), playbackRtDescription.getPlaybackTimeState());
	}
	
	public PlaybackRuntimeDescription convertTo() {
		return PlaybackRuntimeDescription.builder()
				.gatewayId(gatewayId)
				.playbackTimeState(playbackTimeState)
				.build();
	}
}
