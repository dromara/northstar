package org.dromara.northstar.web.restful;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "e2e")
@RestController
public class E2ETestController implements InitializingBean {

	@Autowired
	private RedisTemplate<String, byte[]> redisTemplate;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("准备E2E测试");
		resetDB();
	}
	
	@GetMapping("/resetDB")
	public void resetDB() {
		log.info("重置数据库");
		redisTemplate.delete(redisTemplate.keys("*"));
	}
}
