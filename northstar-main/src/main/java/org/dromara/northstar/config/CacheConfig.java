package org.dromara.northstar.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableCaching
@Configuration
@Profile("!prod")
public class CacheConfig implements InitializingBean{

	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("启用缓存管理");
	}

}
