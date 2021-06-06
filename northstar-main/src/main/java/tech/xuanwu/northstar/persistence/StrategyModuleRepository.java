package tech.xuanwu.northstar.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.strategy.common.model.CtaStrategyModule;

@Repository
public interface StrategyModuleRepository extends MongoRepository<CtaStrategyModule, String>{

}
