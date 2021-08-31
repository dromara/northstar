package tech.xuanwu.northstar.main.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.main.persistence.po.GatewayPO;

@Repository
public interface GatewayRepository extends MongoRepository<GatewayPO, String>{

	
}
