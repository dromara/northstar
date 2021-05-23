package tech.xuanwu.northstar.gateway.sim.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories("tech.xuanwu.northstar.gateway.sim.persistence")
public class SimAccountRepositoryAutoConfiguration {

}
