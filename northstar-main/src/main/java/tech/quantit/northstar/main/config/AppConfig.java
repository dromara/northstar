package tech.quantit.northstar.main.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.google.common.io.Files;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.IDataServiceManager;
import tech.quantit.northstar.common.IHolidayManager;
import tech.quantit.northstar.common.IMailMessageContentHandler;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.ContractDefinition;
import tech.quantit.northstar.common.utils.ContractDefinitionReader;
import tech.quantit.northstar.common.utils.ContractUtils;
import tech.quantit.northstar.data.IContractRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.api.domain.IndexContract;
import tech.quantit.northstar.gateway.api.domain.LatencyDetector;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import tech.quantit.northstar.gateway.playback.PlaybackGatewayFactory;
import tech.quantit.northstar.gateway.sim.trade.SimGatewayFactory;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.main.ExternalJarClassLoader;
import tech.quantit.northstar.main.interceptor.AuthorizationInterceptor;
import tech.quantit.northstar.main.mail.MailDeliveryManager;
import tech.quantit.northstar.main.mail.MailSenderFactory;
import tech.quantit.northstar.main.utils.ModuleFactory;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 配置转换器
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
@Configuration
public class AppConfig implements WebMvcConfigurer {

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
		registry.addInterceptor(new AuthorizationInterceptor()).addPathPatterns("/**")
				.excludePathPatterns("/auth/login");
	}

	@Bean
	public GatewayAndConnectionManager gatewayAndConnectionManager() {
		return new GatewayAndConnectionManager();
	}

	@Bean
	public ContractManager contractManager(IContractRepository contractRepo) throws IOException {
		String fileName = "ContractDefinition.csv";
		String tempPath = System.getProperty("java.io.tmpdir") + "Northstar_" + System.currentTimeMillis();
		String tempFilePath = tempPath + File.separator + fileName;
		Resource resource = new DefaultResourceLoader().getResource("classpath:" + fileName);
		File tempFile = new File(tempFilePath);
		FileUtils.forceMkdirParent(tempFile);
		try (FileOutputStream fos = new FileOutputStream(tempFile)) {
			IOUtils.copy(resource.getInputStream(), fos);
		}

		ContractDefinitionReader reader = new ContractDefinitionReader();
		List<ContractDefinition> contractDefs = reader.load(tempFile);
		ContractManager mgr = new ContractManager(contractDefs);
		contractRepo.findAll().forEach(mgr::addContract);
		return mgr;
	}

	@Bean
	public ConcurrentMap<String, TradeDayAccount> accountMap() {
		return new ConcurrentHashMap<>();
	}

	@Bean
	public SimMarket simMarket() {
		return new SimMarket();
	}

	@ConditionalOnProperty(prefix = "northstar.latency-detection", name = "enabled", havingValue = "true")
	@Bean
	public LatencyDetector latencyDetector(
			@Value("${northstar.latency-detection.sampling-interval}") int samplingInterval) {
		log.info("启用延时探测器");
		return new LatencyDetector(samplingInterval, 3);
	}

	@Bean
	public GlobalMarketRegistry globalRegistry(FastEventEngine fastEventEngine, IContractRepository contractRepo,
			ContractManager contractMgr, @Autowired(required = false) LatencyDetector latencyDetector) {
		Consumer<NormalContract> handleContractSave = contract -> {
			if (contract.updateTime() > 0) {
				contractRepo.save(contract.contractField(), contract.gatewayType());
			}
		};

		GlobalMarketRegistry registry = new GlobalMarketRegistry(fastEventEngine, handleContractSave,
				contractMgr::addContract, latencyDetector);
		// 加载已有合约
		List<ContractField> contractList = contractRepo.findAll();
		Map<String, ContractField> contractMap = contractList.stream()
				.collect(Collectors.toMap(ContractField::getUnifiedSymbol, c -> c));

		for (ContractField contract : contractList) {
			if (contract.getProductClass() == ProductClassEnum.FUTURES
					&& contract.getSymbol().endsWith(Constants.INDEX_SUFFIX)) {
				Set<ContractField> monthlyContracts = new HashSet<>();
				for (String monthlyContractSymbol : ContractUtils
						.getMonthlyUnifiedSymbolOfIndexContract(contract.getUnifiedSymbol(), contract.getExchange())) {
					if (contractMap.containsKey(monthlyContractSymbol)) {
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
	public GatewayFactory ctpGatewayFactory(FastEventEngine fastEventEngine, GlobalMarketRegistry registry, IDataServiceManager dataMgr) {
		return new CtpGatewayFactory(fastEventEngine, registry, dataMgr);
	}

	@Bean
	public GatewayFactory ctpSimGatewayFactory(FastEventEngine fastEventEngine, GlobalMarketRegistry registry, IDataServiceManager dataMgr) {
		return new CtpSimGatewayFactory(fastEventEngine, registry, dataMgr);
	}

	@Bean
	public GatewayFactory playbackGatewayFactory(FastEventEngine fastEventEngine, IContractManager contractMgr,
			IHolidayManager holidayMgr, IMarketDataRepository mdRepo, IPlaybackRuntimeRepository rtRepo) {
		return new PlaybackGatewayFactory(fastEventEngine, contractMgr, holidayMgr, rtRepo, mdRepo);
	}

	@Bean
	public GatewayFactory simGatewayFactory(FastEventEngine fastEventEngine, SimMarket simMarket,
			ISimAccountRepository accRepo, GlobalMarketRegistry registry, ContractManager contractMgr) {
		return new SimGatewayFactory(fastEventEngine, simMarket, accRepo, registry, contractMgr);
	}

	@Bean
	public ExternalJarClassLoader extJarListener() throws MalformedURLException {
		ApplicationHome appHome = new ApplicationHome(getClass());
		File appPath = appHome.getDir();
		ExternalJarClassLoader clzLoader = null;
		for (File file : appPath.listFiles()) {
			if (file.getName().contains("northstar-external")
					&& Files.getFileExtension(file.getName()).equalsIgnoreCase("jar") && !file.isDirectory()) {
				log.info("加载northstar-external扩展包");
				clzLoader = new ExternalJarClassLoader(new URL[] { file.toURI().toURL() }, getClass().getClassLoader());
				clzLoader.initBean();
				break;
			}
		}
		return clzLoader;
	}

	@Bean
	public ModuleFactory moduleFactory(@Autowired(required = false) ExternalJarClassLoader extJarLoader,
			IModuleRepository moduleRepo, GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr) {
		return new ModuleFactory(extJarLoader, moduleRepo, gatewayConnMgr, contractMgr);
	}

	private static OkHttpClient getUnsafeOkHttpClient() {
		try {
			// Create a trust manager that does not validate certificate chains
			final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new java.security.cert.X509Certificate[] {};
				}
			} };

			// Install the all-trusting trust manager
			final SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			OkHttpClient.Builder builder = new OkHttpClient.Builder();
			builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
			builder.hostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});

			OkHttpClient okHttpClient = builder.build();
			return okHttpClient;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate(new OkHttp3ClientHttpRequestFactory(getUnsafeOkHttpClient()));
	}

	@ConditionalOnMissingBean(IMailMessageContentHandler.class)
	@Bean
	public IMailMessageContentHandler messageDeliveryHandler() {
		return new IMailMessageContentHandler() {
		};
	}

	@Bean
	public MailDeliveryManager mailDeliveryManager(IMailMessageContentHandler handler) {
		return new MailDeliveryManager(new MailSenderFactory(), handler);
	}

}
