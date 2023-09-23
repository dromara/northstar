package org.dromara.northstar.support;

import org.dromara.northstar.web.service.GatewayService;
import org.dromara.northstar.web.service.ModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(Ordered.LOWEST_PRECEDENCE - 100)
@Component
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
