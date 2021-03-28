package tech.xuanwu.northstar.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.persistence.po.GatewayPO;

@Repository
public interface GatewayRepository extends MongoRepository<GatewayPO, String>{

	
}
