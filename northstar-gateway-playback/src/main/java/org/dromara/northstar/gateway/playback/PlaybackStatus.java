package org.dromara.northstar.gateway.playback;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PlaybackStatus {

	@Builder.Default
	private volatile State state = State.INITIAL;
	
	private volatile int playInterval;
	
	private volatile LocalDateTime runningDateTime;
	
	public enum State {
		INITIAL,
		PRELOADING,
		PRELOADED,
		PLAYING,
		FINISHED
	}
}
