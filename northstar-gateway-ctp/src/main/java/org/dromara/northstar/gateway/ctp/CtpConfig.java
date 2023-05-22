package org.dromara.northstar.gateway.ctp;

import java.util.Optional;

import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.gateway.IMarketCenter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;

@Slf4j
@Configuration
public class CtpConfig {
	
	static {
		log.info("=====================================================");
		log.info("                  加载gateway-ctp                    ");
		log.info("=====================================================");
	}

	@Value("${northstar.data-service.baseUrl}")
	private String baseUrl;
	
	@Bean
	CtpDataServiceManager ctpDataServiceManager(RestTemplate restTemplate) {
		String nsdsSecret = Optional.ofNullable(System.getenv(Constants.NS_DS_SECRET)).orElse("");
		return new CtpDataServiceManager(baseUrl, nsdsSecret, restTemplate, new CtpDateTimeUtil());
	}
	
	@Bean
	CtpSimDataServiceManager ctpSimDataServiceManager(RestTemplate restTemplate) {
		String nsdsSecret = Optional.ofNullable(System.getenv(Constants.NS_DS_SECRET)).orElse("");
		return new CtpSimDataServiceManager(baseUrl, nsdsSecret, restTemplate, new CtpDateTimeUtil());
	}
	
	@Bean
	CtpGatewayFactory ctpGatewayFactory(FastEventEngine feEngine, IMarketCenter mktCenter,
			@Qualifier("ctpDataServiceManager") CtpDataServiceManager dsMgr) {
		return new CtpGatewayFactory(feEngine, mktCenter, dsMgr);
	}
	
	@Bean
	CtpSimGatewayFactory ctpSimGatewayFactory(FastEventEngine feEngine, IMarketCenter mktCenter, 
			@Qualifier("ctpSimDataServiceManager") CtpSimDataServiceManager dsMgr) {
		return new CtpSimGatewayFactory(feEngine, mktCenter, dsMgr);
	}
}
