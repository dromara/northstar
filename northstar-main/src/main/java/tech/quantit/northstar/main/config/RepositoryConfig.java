package tech.quantit.northstar.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import tech.quantit.northstar.data.IContractRepository;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.mongo.ContractRepoMongoImpl;
import tech.quantit.northstar.data.mongo.GatewayRepoMongoImpl;
import tech.quantit.northstar.data.mongo.MarketDataRepoMongoImpl;
import tech.quantit.northstar.data.mongo.ModuleRepoMongoImpl;

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
	
	@Bean
	public IModuleRepository moduleRepository(MongoTemplate mongo) {
		return new ModuleRepoMongoImpl(mongo);
	}
	
	@Bean
	public IMarketDataRepository marketDataRepository(MongoTemplate mongo) {
		return new MarketDataRepoMongoImpl(mongo);
	}
}
