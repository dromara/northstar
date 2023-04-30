package org.dromara.northstar.data.redis;

import java.util.List;
import java.util.Set;

import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.IGatewayRepository;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.fastjson2.JSON;

/**
 *
 * @author KevinHuangwl
 *
 */
public class GatewayRepoRedisImpl implements IGatewayRepository{

	private RedisTemplate<String, byte[]> redisTemplate;

	private static final String KEY_PREFIX = Constants.APP_NAME + "Gateway:";

	public GatewayRepoRedisImpl(RedisTemplate<String, byte[]> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * redis的数据保存结构
	 * key -> string
	 * key=Gateway:GatewayId
	 * value=json(object)
	 */
	@Override
	public void insert(GatewayDescription gatewayDescription) {
		Set<String> gatewayKeys = redisTemplate.keys(KEY_PREFIX + gatewayDescription.getGatewayId());
		if(!gatewayKeys.isEmpty()) {
			throw new IllegalStateException("已存在同名网关，不能重复创建");
		}
		save(gatewayDescription);
	}

	@Override
	public void save(GatewayDescription gatewayDescription) {
		redisTemplate.boundValueOps(KEY_PREFIX + gatewayDescription.getGatewayId()).set(JSON.toJSONBytes(gatewayDescription));
	}

	@Override
	public void deleteById(String gatewayId) {
		redisTemplate.delete(KEY_PREFIX + gatewayId);
	}

	@Override
	public List<GatewayDescription> findAll() {
		Set<String> gatewayKeys = redisTemplate.keys(KEY_PREFIX+"*");
		return gatewayKeys.stream()
				.map(key -> redisTemplate.boundValueOps(key).get())
				.map(jsonb -> JSON.parseObject(jsonb, GatewayDescription.class))
				.toList();
	}

	@Override
	public GatewayDescription findById(String gatewayId) {
		byte[] data = redisTemplate.boundValueOps(KEY_PREFIX + gatewayId).get();
		if(data == null)	return null;
		return JSON.parseObject(data, GatewayDescription.class);
	}

}
