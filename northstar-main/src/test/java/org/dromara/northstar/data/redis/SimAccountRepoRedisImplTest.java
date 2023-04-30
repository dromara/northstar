package org.dromara.northstar.data.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.model.SimAccountDescription;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.data.redis.SimAccountRepoRedisImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TradeField;

class SimAccountRepoRedisImplTest {

	static LettuceConnectionFactory factory = new LettuceConnectionFactory();
	
	static RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
	
	static ISimAccountRepository repo;
	
	TestFieldFactory fieldFactory = new TestFieldFactory("test");
	
	String KEY_PREFIX = Constants.APP_NAME + "SimAccount:";
	
	String accountId = "testAccount";
	
	TradeField trade = fieldFactory.makeTradeField("rb2210", 1000, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	
	SimAccountDescription simAcc = SimAccountDescription.builder()
			.gatewayId(accountId)
			.totalDeposit(10000)
			.totalCommission(30)
			.openTrades(List.of(trade.toByteArray()))
			.build();
	
	@BeforeEach
	void prepare() {
		factory.setDatabase(15);
		factory.afterPropertiesSet();
		
		redisTemplate.setConnectionFactory(factory);
		redisTemplate.setKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
		redisTemplate.afterPropertiesSet();
		
		repo = new SimAccountRepoRedisImpl(redisTemplate);
	}
	
	@AfterEach
	void cleanup() {
		redisTemplate.delete(redisTemplate.keys("*"));
	}
	
	@Test
	void testSave() {
		repo.save(simAcc);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + accountId)).isTrue();
	}

	@Test
	void testFindById() {
		repo.save(simAcc);
		SimAccountDescription result = repo.findById(accountId);
		assertThat(simAcc.equals(result)).isTrue();
		assertThat(repo.findById(accountId)).isEqualTo(simAcc);
	}

	@Test
	void testDeleteById() {
		repo.save(simAcc);
		repo.deleteById(accountId);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + accountId)).isFalse();
	}

}
