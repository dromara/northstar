package tech.quantit.northstar.main.config;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tech.quantit.northstar.data.IContractRepository;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.redis.ContractRepoRedisImpl;
import tech.quantit.northstar.data.redis.GatewayRepoRedisImpl;
import tech.quantit.northstar.data.redis.MarketDataRepoRedisImpl;
import tech.quantit.northstar.data.redis.ModuleRepoRedisImpl;

@Configuration
public class RepositoryConfig {
	
	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory();
	}
	
	@Bean
	public RedisTemplate<String, byte[]> redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(factory);
		redisTemplate.setKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
		return redisTemplate;
	}

	@Bean
	public IContractRepository contractRepository(RedisTemplate<String, byte[]> redisTemplate) {
		return new ContractRepoRedisImpl(redisTemplate);
	}
	
	@Bean
	public IGatewayRepository gatewayRepository(RedisTemplate<String, byte[]> redisTemplate) {
		return new GatewayRepoRedisImpl(redisTemplate);
	}
	
	@Bean
	public IModuleRepository moduleRepository(RedisTemplate<String, byte[]> redisTemplate) {
		return new ModuleRepoRedisImpl(redisTemplate);
	}
	
	@Bean
	public IMarketDataRepository marketDataRepository(RedisTemplate<String, byte[]> redisTemplate) {
		return new MarketDataRepoRedisImpl(redisTemplate);
	}
	
}
