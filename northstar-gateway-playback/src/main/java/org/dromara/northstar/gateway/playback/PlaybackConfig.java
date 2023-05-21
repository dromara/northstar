package org.dromara.northstar.gateway.playback;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaybackConfig {

	
	
	
	@Bean
	PlaybackDataServiceManager playbackDataServiceManager() {
		
		
		return new PlaybackDataServiceManager(null, null, null, null);
	}
	
	
}
