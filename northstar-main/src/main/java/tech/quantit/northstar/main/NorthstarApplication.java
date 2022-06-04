package tech.quantit.northstar.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAutoConfiguration
@SpringBootApplication(scanBasePackages = "tech.quantit.northstar")
public class NorthstarApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(NorthstarApplication.class, args);
	}

}
