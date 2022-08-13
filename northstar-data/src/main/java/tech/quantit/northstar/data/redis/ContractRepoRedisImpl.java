package tech.quantit.northstar.data.redis;

import java.util.Collections;
import java.util.List;

import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.data.IContractRepository;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ContractRepoRedisImpl implements IContractRepository {
	
	private RedisTemplate<String, byte[]> redisTemplate;
	
	private static final String KEY_PREFIX = Constants.APP_NAME + "Contracts:";
	
	public ContractRepoRedisImpl(RedisTemplate<String, byte[]> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * redis的数据保存结构
	 * key -> hash
	 * key=Contract:GatewayType
	 * value = {
	 * 	unifiedSymbol: contractDataBytes
	 * }
	 */
	@Override
	public void save(ContractField contract, String gatewayType) {
		String key = KEY_PREFIX + gatewayType;
		redisTemplate.boundHashOps(key).put(contract.getUnifiedSymbol(), contract.toByteArray());
	}

	@Override
	public List<ContractField> findAll(String type) {
		String key = KEY_PREFIX + type;
		BoundHashOperations<String, String, byte[]> opt = redisTemplate.boundHashOps(key);
		List<byte[]> results = opt.values();
		if(results == null)
			return Collections.emptyList();
		return results.stream()
				.map(this::convertObject)
				.toList();
	}
	
	private ContractField convertObject(byte[] data) {
		try {
			return ContractField.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			log.warn("", e);
		}
		return null;
	}

	@Override
	public List<ContractField> findAll() {
		String keys = KEY_PREFIX + "*";
		return redisTemplate.keys(keys).stream()
			.map(redisTemplate::boundHashOps)
			.flatMap(opt -> opt.values().stream())
			.map(bytes -> convertObject((byte[])bytes))
			.toList();
	}

}
