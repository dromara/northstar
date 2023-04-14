package org.dromara.northstar.data.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.data.ds.DataServiceManager;
import org.dromara.northstar.data.redis.MarketDataRepoRedisImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.gateway.api.IMarketDataRepository;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;

@SuppressWarnings("unchecked")
class MarketDataRepoRedisImplTest {

	static LettuceConnectionFactory factory = new LettuceConnectionFactory();
	
	static RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
	
	static IMarketDataRepository repo;
	
	String KEY_PREFIX = Constants.APP_NAME + "BarData";
	
	TestFieldFactory fieldFactory = new TestFieldFactory("test");
	
	String date = LocalTime.now().isAfter(LocalTime.of(20, 0)) 
			? LocalDate.now().plusDays(1).format(DateTimeConstant.D_FORMAT_INT_FORMATTER)
			: LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
	
	BarField bar1 = BarField.newBuilder()
			.setGatewayId("CTP")
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setTradingDay(date)
			.build();
	
	BarField bar2 = BarField.newBuilder()
			.setGatewayId("CTP")
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setTradingDay(date)
			.build();
	
	BarField bar3 = BarField.newBuilder()
			.setGatewayId("CTP")
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setTradingDay(date)
			.build();
	
	@BeforeEach
	void prepare() {
		factory.setDatabase(15);
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
	void testInsert() {
		repo.insert(bar1);
		repo.insert(bar2);
		repo.insert(bar3);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + ":" + date + ":rb2210@SHFE@FUTURES")).isTrue();
	}

	// 20:00点整时，跑该测试可能会报错
	@Test
	void testLoadBars() {
		testInsert();
		LocalDate start = LocalTime.now().isAfter(LocalTime.of(20, 0)) ? LocalDate.now() : LocalDate.now().minusDays(1);
		LocalDate end = LocalDate.now().plusWeeks(1);
		List<BarField> result = repo.loadBars("rb2210@SHFE@FUTURES", start, end);
		assertThat(result).hasSize(3);
	}

	// 日期范围没有覆盖当天，数据服务有数据
	@Test
	void testLoadBarsPeriods0() {
		RedisTemplate<String, byte[]> mockRedisTemplate = mock(RedisTemplate.class);
		DataServiceManager mockDataMgr = mock(DataServiceManager.class);
		when(mockDataMgr.getMinutelyData(anyString(), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(BarField.newBuilder()
				.setGatewayId("CTP")
				.setUnifiedSymbol("testSymbol")
				.build()));
		IMarketDataRepository mdRepo = new MarketDataRepoRedisImpl(mockRedisTemplate, mockDataMgr);
		
		mdRepo.loadBars("testSymbol", LocalDate.of(2022, 8, 16), LocalDate.now().minusDays(1));
		verify(mockDataMgr).getMinutelyData(eq("testSymbol"), eq(LocalDate.of(2022, 8, 16)), eq(LocalDate.now().minusDays(1)));
		verify(mockRedisTemplate, times(0)).boundListOps(anyString());
	}
	
	// 日期范围没有覆盖当天，数据服务无数据
	@Test
	void testLoadBarsPeriods1() {
		RedisTemplate<String, byte[]> mockRedisTemplate = mock(RedisTemplate.class);
		DataServiceManager mockDataMgr = mock(DataServiceManager.class);
		when(mockDataMgr.getMinutelyData(anyString(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
		IMarketDataRepository mdRepo = new MarketDataRepoRedisImpl(mockRedisTemplate, mockDataMgr);
		
		mdRepo.loadBars("testSymbol", LocalDate.of(2022, 8, 16), LocalDate.now().minusDays(1));
		verify(mockDataMgr).getMinutelyData(eq("testSymbol"), eq(LocalDate.of(2022, 8, 16)), eq(LocalDate.now().minusDays(1)));
		verify(mockRedisTemplate, times(0)).boundListOps(anyString());
	}
	
	// 日期范围覆盖当天，数据服务没有当天数据
	@Test
	void testLoadBarsPeriods2() {
		RedisTemplate<String, byte[]> mockRedisTemplate = mock(RedisTemplate.class);
		DataServiceManager mockDataMgr = mock(DataServiceManager.class);
		BoundListOperations<String, byte[]> list = mock(BoundListOperations.class);
		when(mockDataMgr.getMinutelyData(anyString(), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(BarField.newBuilder()
					.setGatewayId("CTP")
					.setUnifiedSymbol("testSymbol")
					.build()));
		when(mockRedisTemplate.boundListOps(anyString())).thenReturn(list);
		when(list.size()).thenReturn(0L);
		IMarketDataRepository mdRepo = new MarketDataRepoRedisImpl(mockRedisTemplate, mockDataMgr);
		
		LocalDate endDate = LocalDate.now().plusDays(2);
		while(endDate.getDayOfWeek().getValue() > 5) {
			endDate = endDate.plusDays(1);
		}
		LocalDate today = LocalDate.now();
		LocalDate realDate = today;
		while(realDate.getDayOfWeek().getValue() > 5) {
			realDate = realDate.plusDays(1);
		}
		mdRepo.loadBars("testSymbol", LocalDate.of(2022, 8, 16), endDate);
		verify(mockDataMgr).getMinutelyData(eq("testSymbol"), eq(LocalDate.of(2022, 8, 16)), eq(today));
		verify(mockRedisTemplate).boundListOps(eq(String.format("%s:%s:%s", KEY_PREFIX, realDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), "testSymbol")));
	}
	
	// 日期范围覆盖当天，数据服务有当天数据
	@Test
	void testLoadBarsPeriods3() {
		RedisTemplate<String, byte[]> mockRedisTemplate = mock(RedisTemplate.class);
		DataServiceManager mockDataMgr = mock(DataServiceManager.class);
		BoundListOperations<String, byte[]> list = mock(BoundListOperations.class);
		when(mockDataMgr.getMinutelyData(anyString(), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(BarField.newBuilder()
				.setGatewayId("CTP")
				.setUnifiedSymbol("testSymbol")
				.setTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.build()));
		
		when(mockRedisTemplate.boundListOps(anyString())).thenReturn(list);
		when(list.size()).thenReturn(0L);
		IMarketDataRepository mdRepo = new MarketDataRepoRedisImpl(mockRedisTemplate, mockDataMgr);
		
		LocalDate endDate = LocalDate.now().plusDays(2);
		while(endDate.getDayOfWeek().getValue() > 5) {
			endDate = endDate.plusDays(1);
		}
		LocalDate date = LocalDate.now().plusDays(1);
		while(date.getDayOfWeek().getValue() > 5) {
			date = date.plusDays(1);
		}
		mdRepo.loadBars("testSymbol", LocalDate.of(2022, 8, 16), endDate);
		verify(mockDataMgr).getMinutelyData(eq("testSymbol"), eq(LocalDate.of(2022, 8, 16)), eq(LocalDate.now()));
		verify(mockRedisTemplate).boundListOps(eq(String.format("%s:%s:%s", KEY_PREFIX, date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), "testSymbol")));
	}
	
	// 日期范围覆盖当天，数据服务无数据
	@Test
	void testLoadBarsPeriods4() {
		RedisTemplate<String, byte[]> mockRedisTemplate = mock(RedisTemplate.class);
		DataServiceManager mockDataMgr = mock(DataServiceManager.class);
		BoundListOperations<String, byte[]> list = mock(BoundListOperations.class);
		when(mockDataMgr.getMinutelyData(anyString(), any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());
		when(mockRedisTemplate.boundListOps(anyString())).thenReturn(list);
		when(list.size()).thenReturn(0L);
		IMarketDataRepository mdRepo = new MarketDataRepoRedisImpl(mockRedisTemplate, mockDataMgr);
		
		LocalDate endDate = LocalDate.now().plusDays(2);
		while(endDate.getDayOfWeek().getValue() > 5) {
			endDate = endDate.plusDays(1);
		}
		LocalDate today = LocalDate.now();
		mdRepo.loadBars("testSymbol", LocalDate.of(2022, 8, 16), endDate);
		verify(mockDataMgr).getMinutelyData(eq("testSymbol"), eq(LocalDate.of(2022, 8, 16)), eq(today));
		verify(mockRedisTemplate).boundListOps(eq(String.format("%s:%s:%s", KEY_PREFIX, today.format(DateTimeConstant.D_FORMAT_INT_FORMATTER), "testSymbol")));
	}
	
}
