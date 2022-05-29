package tech.quantit.northstar.data.redis;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.GatewayType;
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
	public void save(ContractField contract, GatewayType gatewayType) {
		String key = KEY_PREFIX + gatewayType;
		redisTemplate.boundHashOps(key).put(contract.getUnifiedSymbol(), contract.toByteArray());
	}

	@Override
	public List<ContractField> findAll(GatewayType type) {
		String key = KEY_PREFIX + type;
		BoundHashOperations<String, String, byte[]> opt = redisTemplate.boundHashOps(key);
		List<byte[]> results = opt.values();
		if(results == null)
			return Collections.emptyList();
		return results.stream()
				.map(this::convertObject)
//				.filter(item -> Objects.nonNull(item) && nonExpired(item))
				.toList();
	}
	
//	private boolean nonExpired(ContractField contract) {
//		if(contract.getSymbol().contains(Constants.INDEX_SUFFIX)) return false;
//		return StringUtils.isNotBlank(expiredDate) && LocalDate.parse(expiredDate, DateTimeConstant.D_FORMAT_INT_FORMATTER).isAfter(LocalDate.now());
//	}
	
	private ContractField convertObject(byte[] data) {
		try {
			return ContractField.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			log.warn("", e);
		}
		return null;
	}

}
