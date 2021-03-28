package tech.xuanwu.northstar.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.persistence.po.StrategyModulePO;

@Repository
public interface StrategyModuleRepository extends MongoRepository<StrategyModulePO, String>{

}
