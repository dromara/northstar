package tech.quantit.northstar.main.config;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMailConfigRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.data.ds.DataServiceManager;
import tech.quantit.northstar.data.ds.W3DataServiceManager;
import tech.quantit.northstar.data.ds.W3MarketDataRepoDataServiceImpl;
import tech.quantit.northstar.data.redis.GatewayRepoRedisImpl;
import tech.quantit.northstar.data.redis.MailConfigRepoRedisImpl;
import tech.quantit.northstar.data.redis.MarketDataRepoRedisImpl;
import tech.quantit.northstar.data.redis.ModuleRepoRedisImpl;
import tech.quantit.northstar.data.redis.PlaybackRuntimeRepoRedisImpl;
import tech.quantit.northstar.data.redis.SimAccountRepoRedisImpl;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.IMarketDataRepository;
import tech.quantit.northstar.gateway.api.utils.MarketDataRepoFactory;
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
    @Value("${northstar.data-service.w3BaseUrl}")
    private String w3BaseUrl;

    @Bean
    @Primary
    DataServiceManager dataServiceManager(RedisTemplate<String, byte[]> redisTemplate, RestTemplate restTemplate, IContractManager contractMgr) {
        String nsdsSecret = Optional.ofNullable(System.getenv(Constants.NS_DS_SECRET)).orElse("");
        return new DataServiceManager(baseUrl, nsdsSecret, restTemplate, new CtpDateTimeUtil(), contractMgr);
    }
    @Bean
    W3DataServiceManager w3DataServiceManager(RestTemplate restTemplate, IContractManager contractMgr) {
        String nsdsSecret = Optional.ofNullable(System.getenv(Constants.NS_DS_SECRET)).orElse("");
        return new W3DataServiceManager(w3BaseUrl, nsdsSecret, restTemplate, new CtpDateTimeUtil(), contractMgr);
    }

    @Bean
    MarketDataRepoFactory marketDataRepository(RedisTemplate<String, byte[]> redisTemplate, DataServiceManager dsMgr,W3DataServiceManager w3dsMgr, IGatewayRepository gatewayRepo) {
        IMarketDataRepository defaultMarketRepo = new MarketDataRepoRedisImpl(redisTemplate, dsMgr);
        IMarketDataRepository w3MarketRepo = new W3MarketDataRepoDataServiceImpl(w3dsMgr);
        Map<ChannelType, IMarketDataRepository> channelRepoMap = new EnumMap<>(ChannelType.class);
        channelRepoMap.put(ChannelType.PLAYBACK, defaultMarketRepo);
        channelRepoMap.put(ChannelType.CTP, defaultMarketRepo);
        channelRepoMap.put(ChannelType.OKX, w3MarketRepo);
        return new MarketDataRepoFactory(channelRepoMap, gatewayRepo);
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
