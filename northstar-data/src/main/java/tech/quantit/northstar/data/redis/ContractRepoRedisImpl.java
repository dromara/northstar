package tech.quantit.northstar.data.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.data.IContractRepository;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 合约信息以list结构保存，每种合约类型使用一个独立的key
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ContractRepoRedisImpl implements IContractRepository {
	
	private RedisTemplate<String, byte[]> redisTemplate;
	
	private static final String PREFIX = "contract:";
	
	private static final List<byte[]> EMPTY_LIST = new ArrayList<>(0);
	
	public ContractRepoRedisImpl(RedisTemplate<String, byte[]> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void batchSave(List<ContractField> contracts) {
		for(ContractField contract : contracts) {
			save(contract);
		}
	}

	@Override
	public void save(ContractField contract) {
		String key = PREFIX + contract.getProductClass();
		redisTemplate.boundListOps(key).rightPush(contract.toByteArray());
	}

	@Override
	public List<ContractField> findAllByType(ProductClassEnum type) {
		String key = PREFIX + type;
		BoundListOperations<String, byte[]> opt = redisTemplate.boundListOps(key);
		return Optional.ofNullable(opt.range(0, opt.size()))
				.orElse(EMPTY_LIST)
				.stream()
				.map(this::convertObject)
				.filter(Objects::nonNull)
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

}
