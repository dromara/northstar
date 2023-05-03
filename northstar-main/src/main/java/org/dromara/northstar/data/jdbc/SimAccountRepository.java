package org.dromara.northstar.data.jdbc;

import org.dromara.northstar.data.jdbc.entity.SimAccountDescriptionDO;
import org.springframework.data.repository.CrudRepository;

public interface SimAccountRepository extends CrudRepository<SimAccountDescriptionDO, String>{

}
