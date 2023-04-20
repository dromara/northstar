package org.dromara.northstar.config;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IMailConfigRepository;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.data.ds.DataServiceManager;
import org.dromara.northstar.data.ds.W3DataServiceManager;
import org.dromara.northstar.data.ds.W3MarketDataRepoDataServiceImpl;
import org.dromara.northstar.data.redis.GatewayRepoRedisImpl;
import org.dromara.northstar.data.redis.MailConfigRepoRedisImpl;
import org.dromara.northstar.data.redis.MarketDataRepoRedisImpl;
import org.dromara.northstar.data.redis.ModuleRepoRedisImpl;
import org.dromara.northstar.data.redis.PlaybackRuntimeRepoRedisImpl;
import org.dromara.northstar.data.redis.SimAccountRepoRedisImpl;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.common.utils.MarketDataRepoFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

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
    
//    @Bean
//    W3DataServiceManager w3DataServiceManager(RestTemplate restTemplate, IContractManager contractMgr) {
//        String nsdsSecret = Optional.ofNullable(System.getenv(Constants.NS_DS_SECRET)).orElse("");
//        return new W3DataServiceManager(w3BaseUrl, nsdsSecret, restTemplate, new CtpDateTimeUtil(), contractMgr);
//    }

    @Bean
    MarketDataRepoFactory marketDataRepository(RedisTemplate<String, byte[]> redisTemplate, DataServiceManager dsMgr, IGatewayRepository gatewayRepo) {
        IMarketDataRepository defaultMarketRepo = new MarketDataRepoRedisImpl(redisTemplate, dsMgr);
//        IMarketDataRepository w3MarketRepo = new W3MarketDataRepoDataServiceImpl(w3dsMgr);
        Map<ChannelType, IMarketDataRepository> channelRepoMap = new EnumMap<>(ChannelType.class);
        channelRepoMap.put(ChannelType.PLAYBACK, defaultMarketRepo);
        channelRepoMap.put(ChannelType.CTP, defaultMarketRepo);
//        channelRepoMap.put(ChannelType.OKX, w3MarketRepo);
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
