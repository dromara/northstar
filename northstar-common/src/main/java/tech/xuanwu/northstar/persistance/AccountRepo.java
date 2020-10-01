package tech.xuanwu.northstar.persistance;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.persistance.po.Account;

@Repository
public interface AccountRepo extends MongoRepository<Account, String>{

	Account findByGatewayId(String gatewayId);
}
