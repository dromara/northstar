package tech.quantit.northstar.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import tech.quantit.northstar.data.IContractRepository;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.mongo.ContractRepoMongoImpl;
import tech.quantit.northstar.data.mongo.GatewayRepoMongoImpl;

@Configuration
public class RepositoryConfig {

	@Bean
	public IContractRepository contractRepository(MongoTemplate mongo) {
		return new ContractRepoMongoImpl(mongo);
	}
	
	@Bean
	public IGatewayRepository gatewayRepository(MongoTemplate mongo) {
		return new GatewayRepoMongoImpl(mongo);
	}
	
}
