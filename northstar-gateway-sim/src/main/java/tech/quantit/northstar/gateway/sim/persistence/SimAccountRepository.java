package tech.quantit.northstar.gateway.sim.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimAccountRepository extends MongoRepository<SimAccountPO, String>{

}
