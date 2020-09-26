package tech.xuanwu.northstar.trader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableAutoConfiguration
@SpringBootApplication
@ComponentScan("tech.xuanwu.northstar")
@EnableMongoRepositories("tech.xuanwu.northstar")
public class NorthstarTraderApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(NorthstarTraderApplication.class, args);
	}

}
