package tech.quantit.northstar.main.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.utils.ContractUtils;
import tech.quantit.northstar.data.IContractRepository;
import tech.quantit.northstar.data.IModuleRepository;
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
import tech.quantit.northstar.main.ExternalJarListener;
import tech.quantit.northstar.main.interceptor.AuthorizationInterceptor;
import tech.quantit.northstar.main.utils.ModuleFactory;
import xyz.redtorch.gateway.ctp.common.CtpSubscriptionManager;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
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
	public ContractManager contractManager(IContractRepository contractRepo) {
		return new ContractManager(contractRepo.findAll());
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
	public GlobalMarketRegistry globalRegistry(FastEventEngine fastEventEngine, IContractRepository contractRepo, List<SubscriptionManager> subMgrs,
			ContractManager contractMgr) {
		Consumer<NormalContract> handleContractSave = contract -> {
			if(contract.updateTime() > 0) {
				contractRepo.save(contract.contractField(), contract.gatewayType());
			}
		};
		
		GlobalMarketRegistry registry = new GlobalMarketRegistry(fastEventEngine, handleContractSave, contractMgr::addContract);
		//　加载已有合约
		List<ContractField> contractList = contractRepo.findAll();
		Map<String, ContractField> contractMap = contractList.stream()
				.collect(Collectors.toMap(ContractField::getUnifiedSymbol, c -> c));
		
		for(ContractField contract : contractList) {
			if(contract.getProductClass() == ProductClassEnum.FUTURES && contract.getSymbol().endsWith(Constants.INDEX_SUFFIX)) {
				Set<ContractField> monthlyContracts = new HashSet<>();
				for(String monthlyContractSymbol : ContractUtils.getMonthlyUnifiedSymbolOfIndexContract(contract.getUnifiedSymbol(), contract.getExchange())) {
					if(contractMap.containsKey(monthlyContractSymbol)) {
						monthlyContracts.add(contractMap.get(monthlyContractSymbol));
					}
				}
				registry.register(new IndexContract(contract.getUnifiedSymbol(), monthlyContracts));
			} else {
				registry.register(new NormalContract(contract, -1));
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
	public ModuleFactory moduleFactory(ExternalJarListener extJarListener, IModuleRepository moduleRepo, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr) {
		return new ModuleFactory(extJarListener, moduleRepo, gatewayConnMgr, contractMgr);
	}
	
	@Bean
	public MessageHandlerManager messageHandlerManager() {
		return new MessageHandlerManager();
	}
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}
}
