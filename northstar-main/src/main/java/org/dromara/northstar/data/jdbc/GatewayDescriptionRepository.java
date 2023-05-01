package org.dromara.northstar.data.jdbc;

import org.dromara.northstar.data.jdbc.model.GatewayDescriptionDO;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GatewayDescriptionRepository extends CrudRepository<GatewayDescriptionDO, String>{

}
