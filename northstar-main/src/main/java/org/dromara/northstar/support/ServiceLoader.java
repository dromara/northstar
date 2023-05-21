package org.dromara.northstar.support;

import org.dromara.northstar.web.service.GatewayService;
import org.dromara.northstar.web.service.ModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(Integer.MAX_VALUE)
@Component
@ConditionalOnExpression("!'${spring.profiles.active}'.equals('test')")
public class ServiceLoader implements CommandLineRunner{

	@Autowired
	GatewayService gatewayService;
	
	@Autowired
	ModuleService moduleService;
	
	@Override
	public void run(String... args) throws Exception {
		gatewayService.postLoad();
		moduleService.postLoad();
	}

}
