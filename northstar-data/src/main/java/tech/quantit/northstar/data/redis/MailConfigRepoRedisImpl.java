package tech.quantit.northstar.data.redis;

import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.fastjson2.JSON;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.model.MailConfigDescription;
import tech.quantit.northstar.data.IMailConfigRepository;

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
		return JSON.parseObject(redisTemplate.boundValueOps(KEY).get(), MailConfigDescription.class) ;
	}

}
