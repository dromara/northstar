package tech.quantit.northstar.data.redis;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
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

	private static final Set<byte[]> EMPTY_SET = new HashSet<>(0);

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
		redisTemplate.boundSetOps(key).add(contract.toByteArray());
	}

	@Override
	public List<ContractField> findAllByType(ProductClassEnum type) {
		String key = PREFIX + type;
		BoundSetOperations<String, byte[]> opt = redisTemplate.boundSetOps(key);
		return Optional.ofNullable(opt.members())
				.orElse(EMPTY_SET)
				.stream()
				.map(this::convertObject)
				.filter(item -> Objects.nonNull(item) && nonExpired(item.getLastTradeDateOrContractMonth()))
				.toList();
	}


	/**
	 * 根据gatewayId查询合约
	 * TODO 待实现
	 *
	 * @param gatewayId
	 * @return
	 */
	@Override
	public List<ContractField> getByGateWayId(String gatewayId){
		String type = "";
		String key = PREFIX + type;
		BoundSetOperations<String, byte[]> opt = redisTemplate.boundSetOps(key);
		return Optional.ofNullable(opt.members())
				.orElse(EMPTY_SET)
				.stream()
				.map(this::convertObject)
				.filter(item -> Objects.nonNull(item) && nonExpired(item.getLastTradeDateOrContractMonth()))
				.toList();
	}

	private boolean nonExpired(String expiredDate) {
		return StringUtils.isNotBlank(expiredDate) && LocalDate.parse(expiredDate, DateTimeConstant.D_FORMAT_INT_FORMATTER).isAfter(LocalDate.now());
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
