package org.dromara.northstar.gateway.playback;

public interface IPlaybackContext {

	void start();
	
	void stop();
	
	boolean isRunning();
	
	void onStopCallback(Runnable callback);
}
