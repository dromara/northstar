package tech.quantit.northstar.main.config;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMailConfigRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.data.ds.DataServiceManager;
import tech.quantit.northstar.data.redis.GatewayRepoRedisImpl;
import tech.quantit.northstar.data.redis.MailConfigRepoRedisImpl;
import tech.quantit.northstar.data.redis.MarketDataRepoRedisImpl;
import tech.quantit.northstar.data.redis.ModuleRepoRedisImpl;
import tech.quantit.northstar.data.redis.PlaybackRuntimeRepoRedisImpl;
import tech.quantit.northstar.data.redis.SimAccountRepoRedisImpl;
import tech.quantit.northstar.gateway.api.IContractManager;
import xyz.redtorch.gateway.ctp.common.CtpDateTimeUtil;

@Configuration
public class RepositoryConfig {

    @Bean
    RedisTemplate<String, byte[]> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
        return redisTemplate;
    }

    @Bean
    IGatewayRepository gatewayRepository(RedisTemplate<String, byte[]> redisTemplate) {
        return new GatewayRepoRedisImpl(redisTemplate);
    }

    @Bean
    IModuleRepository moduleRepository(RedisTemplate<String, byte[]> redisTemplate) {
        return new ModuleRepoRedisImpl(redisTemplate);
    }
	
	@Value("${northstar.data-service.baseUrl}")
	private String baseUrl;

    @Bean
    DataServiceManager dataServiceManager(RedisTemplate<String, byte[]> redisTemplate, RestTemplate restTemplate, IContractManager contractMgr) {
        String nsdsSecret = Optional.ofNullable(System.getenv(Constants.NS_DS_SECRET)).orElse("");
        return new DataServiceManager(baseUrl, nsdsSecret, restTemplate, new CtpDateTimeUtil(), contractMgr);
    }

    @Bean
    IMarketDataRepository marketDataRepository(RedisTemplate<String, byte[]> redisTemplate, DataServiceManager dsMgr) {
        return new MarketDataRepoRedisImpl(redisTemplate, dsMgr);
    }

    @Bean
    ISimAccountRepository simAccountRepository(RedisTemplate<String, byte[]> redisTemplate) {
        return new SimAccountRepoRedisImpl(redisTemplate);
    }

    @Bean
    IPlaybackRuntimeRepository playbackRuntimeRepository(RedisTemplate<String, byte[]> redisTemplate) {
        return new PlaybackRuntimeRepoRedisImpl(redisTemplate);
    }

    @Bean
    IMailConfigRepository mailConfigRepository(RedisTemplate<String, byte[]> redisTemplate) {
        return new MailConfigRepoRedisImpl(redisTemplate);
    }
}
