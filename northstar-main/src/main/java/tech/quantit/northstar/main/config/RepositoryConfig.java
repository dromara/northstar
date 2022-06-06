package tech.quantit.northstar.main.config;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

import tech.quantit.northstar.data.IContractRepository;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.data.ds.DataServiceManager;
import tech.quantit.northstar.data.redis.ContractRepoRedisImpl;
import tech.quantit.northstar.data.redis.GatewayRepoRedisImpl;
import tech.quantit.northstar.data.redis.MarketDataRepoRedisImpl;
import tech.quantit.northstar.data.redis.ModuleRepoRedisImpl;
import tech.quantit.northstar.data.redis.SimAccountRepoRedisImpl;
import xyz.redtorch.gateway.ctp.common.CtpDateTimeUtil;

@Configuration
public class RepositoryConfig {
	
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
	
	@Value("${northstar.data-service.baseUrl}")
	private String baseUrl;
	
	@Value("${northstar.data-service.token}")
	private String token;
	
	@Bean
	public IMarketDataRepository marketDataRepository(RedisTemplate<String, byte[]> redisTemplate, RestTemplate restTemplate) {
		DataServiceManager dsMgr = new DataServiceManager(baseUrl, token, restTemplate, new CtpDateTimeUtil());
		return new MarketDataRepoRedisImpl(redisTemplate, dsMgr);
	}
	
	@Bean
	public ISimAccountRepository simAccountRepository(RedisTemplate<String, byte[]> redisTemplate) {
		return new SimAccountRepoRedisImpl(redisTemplate);
	}

}
