package org.dromara.northstar.data.redis;

import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.fastjson2.JSON;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.model.SimAccountDescription;
import tech.quantit.northstar.data.ISimAccountRepository;

/**
 * 
 * @author KevinHuangwl
 *
 */
public class SimAccountRepoRedisImpl implements ISimAccountRepository {
	
	private RedisTemplate<String, byte[]> redisTemplate;
	
	private static final String KEY_PREFIX = Constants.APP_NAME + "SimAccount:";
	
	public SimAccountRepoRedisImpl(RedisTemplate<String, byte[]> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * redis的数据保存结构
	 * key -> string
	 * key=SimAccount:AccountId
	 * value=json(object)
	 */
	@Override
	public void save(SimAccountDescription simAccountDescription) {
		redisTemplate
			.boundValueOps(KEY_PREFIX + simAccountDescription.getGatewayId())
			.set(JSON.toJSONBytes(simAccountDescription));
	}

	@Override
	public SimAccountDescription findById(String accountId) {
		byte[] data = redisTemplate.boundValueOps(KEY_PREFIX + accountId).get();
		if(data == null) return null;
		return JSON.parseObject(data, SimAccountDescription.class) ;
	}

	@Override
	public void deleteById(String accountId) {
		redisTemplate.delete(KEY_PREFIX + accountId);
	}

}
