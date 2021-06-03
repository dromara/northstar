package tech.xuanwu.northstar.strategy.cta.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.strategy.cta.model.CtaStrategyModule;

@Repository
public interface StrategyModuleRepository extends MongoRepository<CtaStrategyModule, String>{

}
