package tech.xuanwu.northstar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAutoConfiguration
@SpringBootApplication
@EnableMongoRepositories("tech.xuanwu.northstar")
public class NorthstarApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(NorthstarApplication.class, args);
	}

}
