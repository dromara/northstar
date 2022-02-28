package tech.quantit.northstar.main.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

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

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.external.MessageHandlerManager;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.api.domain.IndexContract;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import tech.quantit.northstar.gateway.api.domain.SubscriptionManager;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountRepository;
import tech.quantit.northstar.gateway.sim.trade.SimGatewayFactory;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.main.MarketDataCache;
import tech.quantit.northstar.main.interceptor.AuthorizationInterceptor;
import tech.quantit.northstar.main.persistence.IMarketDataRepository;
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
	public ContractManager contractManager() {
		return new ContractManager();
	}
	
	@Bean
	public ConcurrentMap<String, TradeDayAccount> accountMap(){
		return new ConcurrentHashMap<>();
	}
	
	@Bean
	public SimMarket simMarket(SimAccountRepository simAccRepo) {
		return new SimMarket(simAccRepo);
	}
	
	@Bean
	public SubscriptionManager ctpSubscriptionManager(SubscriptionConfigurationProperties props) {
		return new CtpSubscriptionManager(props.getClzTypeWhtlist(), props.getClzTypeBlklist(), props.getSymbolWhtlist(), props.getSymbolBlklist());
	}
	
	@Bean
	public GlobalMarketRegistry marketGlobalRegistry(FastEventEngine fastEventEngine, IMarketDataRepository mdRepo, List<SubscriptionManager> subMgrs,
			MarketDataCache mdCache, ContractManager contractMgr) throws InvalidProtocolBufferException {
		Consumer<NormalContract> handleContractSave = contract -> {
			if(System.currentTimeMillis() - contract.updateTime() < 60000) {
				// 更新时间少于一分钟的合约才是需要保存新增合约		
				Set<String> monthlyContractSymbols = null;
				boolean isIndexContract = false;
				if(contract instanceof IndexContract idxContract) {
					isIndexContract = true;
					monthlyContractSymbols = idxContract.monthlyContractSymbols();
				}
				ContractPO po = ContractPO.builder()
						.unifiedSymbol(contract.unifiedSymbol())
						.data(contract.contractField().toByteArray())
						.gatewayType(contract.gatewayType())
						.updateTime(contract.updateTime())
						.isIndexContract(isIndexContract)
						.monthlyContractSymbols(monthlyContractSymbols)
						.build();
				mdRepo.saveContract(po);
			}
		};
		
		GlobalMarketRegistry registry = new GlobalMarketRegistry(fastEventEngine, handleContractSave, contractMgr::addContract, mdCache);
		// 加载合约订阅管理器
		for(SubscriptionManager subMgr : subMgrs) {			
			registry.register(subMgr);
		}
		//　加载已有合约
		List<ContractPO> contractList = mdRepo.getAvailableContracts();
		Map<String, ContractField> contractMap = new HashMap<>();
		for(ContractPO po : contractList) {
			ContractField contract = ContractField.parseFrom(po.getData());
			contractMap.put(contract.getUnifiedSymbol(), contract);
		}
		for(ContractPO po : contractList) {
			ContractField contract = contractMap.get(po.getUnifiedSymbol());
			if(po.isIndexContract()) {
				Set<ContractField> monthlyContracts = new HashSet<>();
				for(String monthlyContractSymbol : po.getMonthlyContractSymbols()) {
					if(contractMap.containsKey(monthlyContractSymbol)) {
						monthlyContracts.add(contractMap.get(monthlyContractSymbol));
					}
				}
				registry.register(new IndexContract(contract.getUnifiedSymbol(), po.getGatewayType(), monthlyContracts));
			} else {
				registry.register(new NormalContract(contract, po.getGatewayType(), po.getUpdateTime()));
			}
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
	
	@Bean
	public MessageHandlerManager messageHandlerManager() {
		return new MessageHandlerManager();
	}
}
