package org.dromara.northstar.data.redis;

import java.util.Optional;

import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.model.MailConfigDescription;
import org.dromara.northstar.data.IMailConfigRepository;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.fastjson2.JSON;

public class MailConfigRepoRedisImpl implements IMailConfigRepository{

	private RedisTemplate<String, byte[]> redisTemplate;

	private static final String KEY = Constants.APP_NAME + "MailConfig";

	public MailConfigRepoRedisImpl(RedisTemplate<String, byte[]> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	@Override
	public void save(MailConfigDescription configDescription) {
		redisTemplate.boundValueOps(KEY).set(JSON.toJSONBytes(configDescription));
	}

	@Override
	public MailConfigDescription get() {
		byte[] data = redisTemplate.boundValueOps(KEY).get();
		return JSON.parseObject(Optional.ofNullable(data).orElse("{}".getBytes()), MailConfigDescription.class) ;
	}

}
