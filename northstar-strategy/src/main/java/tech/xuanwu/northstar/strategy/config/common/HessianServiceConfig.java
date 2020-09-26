package tech.xuanwu.northstar.strategy.config.common;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import tech.xuanwu.northstar.service.ITradeService;
import tech.xuanwu.northstar.strategy.annotation.NorthstarService;

@Component
public class HessianServiceConfig {

	@NorthstarService
	ITradeService tradeService;
	
	@Bean
	public ITradeService getTradeService() {
		return tradeService;
	}
	
}
