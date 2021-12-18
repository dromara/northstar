package tech.quantit.northstar.main.config;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mongodb.client.MongoClient;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.domain.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import tech.quantit.northstar.gateway.api.domain.SubscriptionManager;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountRepository;
import tech.quantit.northstar.gateway.sim.trade.SimGatewayFactory;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.main.interceptor.AuthorizationInterceptor;
import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.MongoClientAdapter;
import tech.quantit.northstar.main.persistence.po.ContractPO;
import xyz.redtorch.gateway.ctp.common.CtpSubscriptionManager;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 配置转换器
 * @author KevinHuangwl
 *
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		/**
		 * 调整转换器优先级
		 */
		List<HttpMessageConverter<?>> jacksonConverters = new ArrayList<>();
		Iterator<HttpMessageConverter<?>> itCvt = converters.iterator();
		while(itCvt.hasNext()) {
			HttpMessageConverter<?> cvt = itCvt.next();
			if(cvt instanceof MappingJackson2HttpMessageConverter) {
				jacksonConverters.add(cvt);
				itCvt.remove();
			}
		}
		for(HttpMessageConverter<?> cvt : jacksonConverters) {
			converters.add(0, cvt);
		}
	}
	
	@Bean
    public CorsFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();
        // 设置允许跨域请求的域名
        config.addAllowedOriginPattern("*");
        // 是否允许证书 不再默认开启
         config.setAllowCredentials(true);
        // 设置允许的方法
        config.addAllowedMethod("*");
        // 允许任何头
        config.addAllowedHeader("*");
        config.addExposedHeader("token");
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", config);
        return new CorsFilter(configSource);
    }
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AuthorizationInterceptor()).addPathPatterns("/**").excludePathPatterns("/auth/login");
	}
	
	@Bean
	public GatewayAndConnectionManager gatewayAndConnectionManager() {
		return new GatewayAndConnectionManager();
	}
	
	@Bean
	public ContractManager contractManager(GlobalMarketRegistry registry, List<SubscriptionManager> subMgrs, MarketDataRepository mdRepo)
			throws InvalidProtocolBufferException {
		ContractManager contractMgr = new ContractManager(registry);
		List<ContractPO> poList = mdRepo.getAvailableContracts();
		Map<GatewayType, SubscriptionManager> subMgrMap = new EnumMap<>(GatewayType.class);
		for(SubscriptionManager subMgr : subMgrs) {
			subMgrMap.put(subMgr.usedFor(), subMgr);
		}
		for(ContractPO po : poList) {
			ContractField contract = ContractField.parseFrom(po.getData());
			SubscriptionManager subMgr = subMgrMap.get(po.getGatewayType()); 
			if(subMgr!= null && !subMgr.subscribable(new NormalContract(contract, po.getGatewayType()))) {
				//如果订阅管理器有定义且配置了不能订阅的情况，则跳过
				continue;
			}
			contractMgr.addContract(contract);
		}
		return contractMgr;
	}
	
	@Bean
	public ConcurrentMap<String, TradeDayAccount> accountMap(){
		return new ConcurrentHashMap<>();
	}
	
	@Bean
	public SimMarket simMarket(SimAccountRepository simAccRepo) {
		return new SimMarket();
	}
	
	@Value("${northstar.subscription.ctp.classType.whitelist:}")
	private String clzTypeWhtlist;
	@Value("${northstar.subscription.ctp.classType.blacklist:}")
	private String clzTypeBlklist;
	@Value("${northstar.subscription.ctp.unifiedSymbol.whitelist:}")
	private String symbolWhtlist;
	@Value("${northstar.subscription.ctp.unifiedSymbol.blacklist:}")
	private String symbolBlklist;
	
	@Bean
	public SubscriptionManager ctpSubscriptionManager() {
		return new CtpSubscriptionManager(clzTypeWhtlist, clzTypeBlklist, symbolWhtlist, symbolBlklist);
	}
	
	@Bean
	public GlobalMarketRegistry marketGlobalRegistry(FastEventEngine fastEventEngine, MarketDataRepository mdRepo, List<SubscriptionManager> subMgrs) {
		GlobalMarketRegistry registry = new GlobalMarketRegistry(fastEventEngine,
				contract -> mdRepo.saveContract(
						new ContractPO(
							contract.contractField().getUnifiedSymbol(),
							contract.contractField().toByteArray(), 
							contract.gatewayType(),
							System.currentTimeMillis()
							)));
		for(SubscriptionManager subMgr : subMgrs) {			
			registry.register(subMgr);
		}
		return registry;
	}
	
	@Bean
	public GatewayFactory ctpGatewayFactory(FastEventEngine fastEventEngine, GlobalMarketRegistry registry) {
		return new CtpGatewayFactory(fastEventEngine, registry);
	}
	
	@Bean
	public GatewayFactory ctpSimGatewayFactory(FastEventEngine fastEventEngine, GlobalMarketRegistry registry) {
		return new CtpSimGatewayFactory(fastEventEngine, registry);
	}
	
	@Bean
	public GatewayFactory simGatewayFactory(FastEventEngine fastEventEngine, SimMarket simMarket, SimAccountRepository accRepo,
			GlobalMarketRegistry registry) {
		return new SimGatewayFactory(fastEventEngine, simMarket, accRepo, registry);
	}
	
	@Bean 
	public MongoClientAdapter mongoClientAdapter(MongoClient mongoClient) {
		return new MongoClientAdapter(mongoClient);
	}
}
