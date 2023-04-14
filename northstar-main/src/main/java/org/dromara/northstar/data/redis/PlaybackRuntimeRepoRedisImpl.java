	package org.dromara.northstar.data.redis;

import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.fastjson2.JSON;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.model.PlaybackRuntimeDescription;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;

/**
 * 
 * @author KevinHuangwl
 *
 */
public class PlaybackRuntimeRepoRedisImpl implements IPlaybackRuntimeRepository{
	
	private RedisTemplate<String, byte[]> redisTemplate;
	
	private static final String KEY_PREFIX = Constants.APP_NAME + "PlaybackRuntime:";
	
	public PlaybackRuntimeRepoRedisImpl(RedisTemplate<String, byte[]> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	@Override
	public void save(PlaybackRuntimeDescription playbackRtDescription) {
		redisTemplate
			.boundValueOps(KEY_PREFIX + playbackRtDescription.getGatewayId())
			.set(JSON.toJSONBytes(playbackRtDescription));
	}

	@Override
	public PlaybackRuntimeDescription findById(String playbackGatewayId) {
		byte[] data = redisTemplate.boundValueOps(KEY_PREFIX + playbackGatewayId).get();
		if(data == null) return null;
		return JSON.parseObject(data, PlaybackRuntimeDescription.class) ;
	}

	@Override
	public void deleteById(String playbackGatewayId) {
		redisTemplate.delete(KEY_PREFIX + playbackGatewayId);
	}

}
