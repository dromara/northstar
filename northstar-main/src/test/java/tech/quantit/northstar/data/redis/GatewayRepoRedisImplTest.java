package tech.quantit.northstar.data.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.dromara.northstar.data.redis.GatewayRepoRedisImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.data.IGatewayRepository;
import test.common.TestFieldFactory;

class GatewayRepoRedisImplTest {

	static LettuceConnectionFactory factory = new LettuceConnectionFactory();
	
	static RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
	
	static IGatewayRepository repo;
	
	TestFieldFactory fieldFactory = new TestFieldFactory("test");
	
	String KEY_PREFIX = Constants.APP_NAME + "Gateway:";
	
	String gatewayId = "testGateway";
	
	GatewayDescription gd = GatewayDescription.builder()
			.gatewayId(gatewayId)
			.settings(new Object())
			.build();
	
	GatewayDescription gd2 = GatewayDescription.builder()
			.gatewayId(gatewayId + "2")
			.settings(new Object())
			.build();
	
	@BeforeEach
	void prepare() {
		factory.setDatabase(15);
		factory.afterPropertiesSet();
		
		redisTemplate.setConnectionFactory(factory);
		redisTemplate.setKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
		redisTemplate.afterPropertiesSet();
		
		repo = new GatewayRepoRedisImpl(redisTemplate);
	}
	
	@AfterEach
	void cleanup() {
		redisTemplate.delete(redisTemplate.keys("*"));
	}
	
	@Test
	void testInsert() {
		repo.insert(gd);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + gatewayId)).isTrue();
		assertThrows(IllegalStateException.class, () -> {
			repo.insert(gd);
		});
	}

	@Test
	void testSave() {
		repo.save(gd);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + gatewayId));
	}

	@Test
	void testDeleteById() {
		testSave();
		repo.deleteById(gatewayId);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + gatewayId)).isFalse();
	}

	@Test
	void testFindAll() {
		repo.insert(gd);
		repo.insert(gd2);
		assertThat(repo.findAll()).hasSize(2);
	}

	@Test
	void testFindById() {
		repo.insert(gd);
		repo.insert(gd2);
		assertThat(repo.findById(gatewayId)).isNotNull();
		
		assertThat(repo.findById("somethingNotExist")).isNull();
	}

}
