package tech.quantit.northstar.data.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.ds.DataServiceManager;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;

class MarketDataRepoRedisImplTest {

	static LettuceConnectionFactory factory = new LettuceConnectionFactory();
	
	static RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
	
	static IMarketDataRepository repo;
	
	String KEY_PREFIX = Constants.APP_NAME + "BarData:";
	
	TestFieldFactory fieldFactory = new TestFieldFactory("test");
	
	String date = LocalTime.now().isAfter(LocalTime.of(20, 0)) 
			? LocalDate.now().plusDays(1).format(DateTimeConstant.D_FORMAT_INT_FORMATTER)
			: LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
	
	BarField bar1 = BarField.newBuilder()
			.setGatewayId("testGateway")
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setTradingDay(date)
			.build();
	
	BarField bar2 = BarField.newBuilder()
			.setGatewayId("testGateway")
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setTradingDay(date)
			.build();
	
	BarField bar3 = BarField.newBuilder()
			.setGatewayId("testGateway")
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setTradingDay(date)
			.build();
	
	@BeforeEach
	void prepare() {
		factory.afterPropertiesSet();
		
		redisTemplate.setConnectionFactory(factory);
		redisTemplate.setKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
		redisTemplate.afterPropertiesSet();
		
		repo = new MarketDataRepoRedisImpl(redisTemplate, mock(DataServiceManager.class));
	}
	
	@AfterEach
	void cleanup() {
		redisTemplate.delete(redisTemplate.keys("*"));
	}
	
	@Test
	void testDropGatewayData() {
		testInsert();
		repo.dropGatewayData("testGateway");
		assertThat(redisTemplate.hasKey(KEY_PREFIX + "testGateway:" + date + ":rb2210@SHFE@FUTURES")).isFalse();
	}

	@Test
	void testInsert() {
		repo.insert(bar1);
		repo.insert(bar2);
		repo.insert(bar3);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + "testGateway:" + date + ":rb2210@SHFE@FUTURES")).isTrue();
	}

	// 20:00点整时，跑该测试可能会报错
	@Test
	void testLoadBars() {
		testInsert();
		LocalDate start = LocalTime.now().isAfter(LocalTime.of(20, 0)) ? LocalDate.now() : LocalDate.now().minusDays(1);
		LocalDate end = LocalDate.now().plusWeeks(1);
		List<BarField> result = repo.loadBars("testGateway", "rb2210@SHFE@FUTURES", start, end);
		assertThat(result).hasSize(3);
	}

}
