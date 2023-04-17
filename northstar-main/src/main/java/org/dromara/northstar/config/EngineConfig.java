package org.dromara.northstar.config;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.event.DisruptorFastEventEngine;
import org.dromara.northstar.event.DisruptorFastEventEngine.WaitStrategyEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * 引擎配置
 * @author KevinHuangwl
 *
 */
@Slf4j
@Configuration
class EngineConfig {

	@Bean
	FastEventEngine eventEngine() throws Exception {
		log.debug("创建EventEngine");
		return new DisruptorFastEventEngine(WaitStrategyEnum.BlockingWaitStrategy);
	}
	
}
