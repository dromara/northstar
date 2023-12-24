package org.dromara.northstar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableCaching
@EnableScheduling
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "org.dromara.northstar.data.jdbc")
@SpringBootApplication
public class NorthstarApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(NorthstarApplication.class, args);
	}

}
