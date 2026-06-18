package org.dromara.northstar.config;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.app.auth.AuthCheckerInterceptor;
import org.dromara.northstar.app.security.MaliciousIPCollector;
import org.dromara.northstar.app.security.MaliciousRequestFilter;
import org.dromara.northstar.common.model.ContractConfigDTO;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.utils.LocalEnvUtils;
import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.dromara.northstar.data.jdbc.ContractConfigRepository;
import org.dromara.northstar.data.jdbc.entity.ContractConfigDO;
import org.dromara.northstar.gateway.IContractDefinitionProvider;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.gateway.mktdata.QuantitDataServiceManager;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import lombok.extern.slf4j.Slf4j;

/**
 * 配置转换器
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
@EnableCaching
@Configuration
public class AppConfig implements WebMvcConfigurer, InitializingBean {

	@Value("${spring.profiles.active}")
	private String profile;
	
	@Autowired
	private Environment env;
	
	@Value("${northstar.data-service.baseUrl}")
	private String baseUrl;
	
	@Value("${quantit.datasource.secret:}")
	private String dsSecret;
	
	@Autowired
	private AuthCheckerInterceptor authChecker;
	
	@Bean
    public LocaleResolver localeResolver() {
        return new AcceptHeaderLocaleResolver();
    }
	
	@Bean
	public MessageSource messageSource() {
	    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
	    messageSource.setBasename("i18n/messages");
	    messageSource.setDefaultEncoding("UTF-8");
	    MessageSourceHolder.setMessageSource(messageSource);
	    return messageSource;
	}
	
	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		/**
		 * 调整转换器优先级
		 */
		List<HttpMessageConverter<?>> jacksonConverters = new ArrayList<>();
		Iterator<HttpMessageConverter<?>> itCvt = converters.iterator();
		while (itCvt.hasNext()) {
			HttpMessageConverter<?> cvt = itCvt.next();
			if (cvt instanceof MappingJackson2HttpMessageConverter) {
				jacksonConverters.add(cvt);
				itCvt.remove();
			}
		}
		for (HttpMessageConverter<?> cvt : jacksonConverters) {
			converters.add(0, cvt);
		}
	}
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		if(env.matchesProfiles("!unittest")) {			
			registry.addInterceptor(authChecker).addPathPatterns("/northstar/**")
				.excludePathPatterns("/northstar/user/**", "/northstar/reset");
		}
	}

    @Bean
    CorsFilter corsFilter() {

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

    @Bean
    MarketCenter marketCenter(List<IContractDefinitionProvider> contractDefProviders, ContractConfigRepository repo) {
    		List<ContractConfigDTO> configDTOs = repo.findAll().stream().map(ContractConfigDO::convertTo).toList();
    		Map<Identifier, ContractConfigDTO> configMap = configDTOs
    				.stream().collect(Collectors.toMap(item -> Identifier.of(item.getContractId()), item -> item));
        return new MarketCenter(contractDefProviders, configMap);
    }
    
    @Bean
    EventEmitterHolder sseEmitter() {
    		return new EventEmitterHolder();
    }
    
    @Bean
    FilterRegistrationBean<MaliciousRequestFilter> maliciousRequestFilter(MaliciousIPCollector colltr){
    	FilterRegistrationBean<MaliciousRequestFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new MaliciousRequestFilter(colltr));
        registrationBean.addUrlPatterns("/*"); 
        registrationBean.setOrder(1); 
        return registrationBean;
    }

    @Bean
    RestTemplate restTemplate(BuildProperties buildProperties) {
	    	String token = System.getenv("NS_DS_SECRET");
	    	if(StringUtils.isNotBlank(dsSecret)) {
	    		token = dsSecret;
	    	}
	    	String partialToken = "空";
	    	if(StringUtils.isNotBlank(token)) {
	    		partialToken = token.split("\\.")[2];
	    	}
	    	log.info("数据服务token验证码: {}", partialToken);
        return new RestTemplateBuilder()
                .setReadTimeout(Duration.ofSeconds(60))
                .setConnectTimeout(Duration.ofSeconds(10))
                .rootUri(baseUrl)
				.defaultHeader("Authorization", String.format("Bearer %s", token))
				.defaultHeader("NS_Version", buildProperties.getVersion())
                .build();
    }

    @Bean
    @ConditionalOnExpression("systemEnvironment['IDEA_INITIAL_DIRECTORY'] == null")
    CommandLineRunner printVersionInfo(BuildProperties buildProperties) {
    	return args -> log.info("Version: {}, Build Time: {}", buildProperties.getVersion(), buildProperties.getTime().atOffset(ZoneOffset.ofHours(8)));
	}
    
    @Bean
    UserInfo userInfo() {
    	return new UserInfo();
    }
    
    @Bean
	QuantitDataServiceManager quantitDataManager(RestTemplate restTemplate) {
		return new QuantitDataServiceManager(restTemplate);
	}
    
    @Bean
    NorthstarDataSource northstarDataSource(QuantitDataServiceManager dsMgr) {
    	return new NorthstarDataSource(dsMgr);
    }

	@Override
	public void afterPropertiesSet() throws Exception {
		LocalEnvUtils.setEnvironment(env);
		log.info("设置全局环境信息");
	}

}