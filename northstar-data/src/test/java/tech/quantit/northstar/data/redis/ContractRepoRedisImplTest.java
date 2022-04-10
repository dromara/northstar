package tech.quantit.northstar.data.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tech.quantit.northstar.data.IContractRepository;
/*
 * 单元测试主要验证这几个方面：
 * 1. redis有反应
 * 2. key命名正确
 * 3. 哪些数据配置自动过期，哪些数据永不过期
 * */
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;


class ContractRepoRedisImplTest {
	
	static LettuceConnectionFactory factory = new LettuceConnectionFactory();
	
	static RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
	
	static IContractRepository repo;
	
	TestFieldFactory fieldFactory = new TestFieldFactory("test");
	
	ContractField c1 = ContractField.newBuilder()
			.setUnifiedSymbol("rb2205@SHFE@FUTURES")
			.setSymbol("rb2205")
			.setProductClass(ProductClassEnum.FUTURES)
			.build();
	
	ContractField c2 = ContractField.newBuilder()
			.setUnifiedSymbol("rb2205-C4000@SHFE@FUTURES")
			.setSymbol("rb2205-C4000")
			.setProductClass(ProductClassEnum.OPTION)
			.build();
	
	@BeforeAll
	static void prepare() {
		factory.afterPropertiesSet();
		
		redisTemplate.setConnectionFactory(factory);
		redisTemplate.setKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
		redisTemplate.afterPropertiesSet();
		
		repo = new ContractRepoRedisImpl(redisTemplate);
	}
	
	@AfterEach
	void clear() {
		redisTemplate.delete(redisTemplate.keys("*"));
	}

	@Test
	void testBatchSave() {
		repo.batchSave(List.of(c1,c2));
		assertThat(redisTemplate.hasKey("contract:FUTURES")).isTrue();
		assertThat(redisTemplate.hasKey("contract:OPTION")).isTrue();
	}

	@Test
	void testSave() {
		repo.save(c2);
		assertThat(redisTemplate.hasKey("contract:OPTION")).isTrue();
		assertThat(redisTemplate.getExpire("contract:OPTION")).isNegative();
	}

	@Test
	void testFindAllByType() {
		repo.batchSave(List.of(c1,c2));
		
		List<ContractField> list = repo.findAllByType(ProductClassEnum.FUTURES);
		assertThat(list).hasSize(1);
		assertThat(list.get(0)).isEqualTo(c1);
	}
	
	@Test
	void shouldOnlyCreateOnce() {
		repo.save(c1);
		repo.save(c1);
		List<ContractField> list = repo.findAllByType(ProductClassEnum.FUTURES);
		assertThat(list).hasSize(1);
		assertThat(list.get(0)).isEqualTo(c1);
	}

}
