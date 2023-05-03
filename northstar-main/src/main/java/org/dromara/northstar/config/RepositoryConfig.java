package org.dromara.northstar.config;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.dromara.northstar.common.IDataServiceManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IMailConfigRepository;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.data.ds.DataServiceManager;
import org.dromara.northstar.data.jdbc.GatewayDescriptionRepository;
import org.dromara.northstar.data.jdbc.GatewayRepoAdapter;
import org.dromara.northstar.data.jdbc.MailConfigDescriptionRepository;
import org.dromara.northstar.data.jdbc.MailConfigRepoAdapter;
import org.dromara.northstar.data.jdbc.MarketDataRepoAdapter;
import org.dromara.northstar.data.jdbc.MarketDataRepository;
import org.dromara.northstar.data.jdbc.ModuleDealRecordRepository;
import org.dromara.northstar.data.jdbc.ModuleDescriptionRepository;
import org.dromara.northstar.data.jdbc.ModuleRepoAdapter;
import org.dromara.northstar.data.jdbc.ModuleRuntimeDescriptionRepository;
import org.dromara.northstar.data.jdbc.PlaybackRuntimeRepoAdapter;
import org.dromara.northstar.data.jdbc.PlaybackRuntimeRepository;
import org.dromara.northstar.data.jdbc.SimAccountRepoAdapter;
import org.dromara.northstar.data.jdbc.SimAccountRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.common.utils.MarketDataRepoFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import xyz.redtorch.gateway.ctp.common.CtpDateTimeUtil;

@Configuration
public class RepositoryConfig {

	@Value("${northstar.data-service.baseUrl}")
	private String baseUrl;
    @Value("${northstar.data-service.w3BaseUrl}")
    private String w3BaseUrl;

    @Bean
    @Primary
    DataServiceManager dataServiceManager(RestTemplate restTemplate, IContractManager contractMgr) {
        String nsdsSecret = Optional.ofNullable(System.getenv(Constants.NS_DS_SECRET)).orElse("");
        return new DataServiceManager(baseUrl, nsdsSecret, restTemplate, new CtpDateTimeUtil(), contractMgr);
    }
    
//    @Bean
//    W3DataServiceManager w3DataServiceManager(RestTemplate restTemplate, IContractManager contractMgr) {
//        String nsdsSecret = Optional.ofNullable(System.getenv(Constants.NS_DS_SECRET)).orElse("");
//        return new W3DataServiceManager(w3BaseUrl, nsdsSecret, restTemplate, new CtpDateTimeUtil(), contractMgr);
//    }

    @Bean
    MarketDataRepoFactory marketDataRepoFactory(IDataServiceManager dsDelegate, MarketDataRepository dbDelegate, IGatewayRepository gatewayRepo) {
        IMarketDataRepository defaultMarketRepo = new MarketDataRepoAdapter(dbDelegate, dsDelegate);
//        IMarketDataRepository w3MarketRepo = new W3MarketDataRepoDataServiceImpl(w3dsMgr);
        Map<ChannelType, IMarketDataRepository> channelRepoMap = new EnumMap<>(ChannelType.class);
        channelRepoMap.put(ChannelType.PLAYBACK, defaultMarketRepo);
        channelRepoMap.put(ChannelType.CTP, defaultMarketRepo);
//        channelRepoMap.put(ChannelType.OKX, w3MarketRepo);
        return new MarketDataRepoFactory(channelRepoMap, gatewayRepo);
    }
    
    @Bean
    IGatewayRepository gatewayRepo(GatewayDescriptionRepository delelgate) {
    	return new GatewayRepoAdapter(delelgate);
    }
    
    @Bean
    IModuleRepository moduleRepo(ModuleDealRecordRepository mdrRepo, ModuleDescriptionRepository mdRepo, ModuleRuntimeDescriptionRepository mrdRepo) {
    	return new ModuleRepoAdapter(mdRepo, mrdRepo, mdrRepo);
    }
    
    @Bean
    IMailConfigRepository mailConfigRepo(MailConfigDescriptionRepository delegate) {
    	return new MailConfigRepoAdapter(delegate);
    }
    
    @Bean
    IPlaybackRuntimeRepository playbackRtRepo(PlaybackRuntimeRepository delegate) {
    	return new PlaybackRuntimeRepoAdapter(delegate);
    }
    
    @Bean
    ISimAccountRepository simAccountRepo(SimAccountRepository delegate) {
    	return new SimAccountRepoAdapter(delegate);
    }
}
