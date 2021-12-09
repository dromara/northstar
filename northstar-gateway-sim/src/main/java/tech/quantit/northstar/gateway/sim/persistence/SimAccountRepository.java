package tech.quantit.northstar.gateway.sim.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import tech.quantit.northstar.gateway.sim.trade.SimAccount;

@Repository
public interface SimAccountRepository extends MongoRepository<SimAccount, String>{

}
