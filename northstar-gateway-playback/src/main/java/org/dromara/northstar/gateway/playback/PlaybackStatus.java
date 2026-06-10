package org.dromara.northstar.gateway.playback;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaybackStatus {

	@Getter
	@Builder.Default
	private volatile State state = State.INITIAL;
	
	@Getter
	private volatile int playInterval;
	
	@Getter
	private volatile LocalDateTime runningDateTime;
	
	@Setter
	private Consumer<PlaybackStatus> onStatusChangeCallback;
	
	public enum State {
		INITIAL,
		PRELOADING,
		PRELOADED,
		PLAYING,
		FINISHED
	}

	public void setState(State state) {
		this.state = state;
		if(onStatusChangeCallback != null) {
			onStatusChangeCallback.accept(this);
		}
	}

	public void setRunningDateTime(LocalDateTime runningDateTime) {
		this.runningDateTime = runningDateTime;
		if(onStatusChangeCallback != null) {
			onStatusChangeCallback.accept(this);
		}
	}

	public void setPlayInterval(int playInterval) {
		this.playInterval = playInterval;
		if(onStatusChangeCallback != null) {
			onStatusChangeCallback.accept(this);
		}
	}
	
}
