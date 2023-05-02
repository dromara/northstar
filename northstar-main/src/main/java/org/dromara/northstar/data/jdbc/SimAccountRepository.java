package org.dromara.northstar.data.jdbc;

import org.dromara.northstar.data.jdbc.model.SimAccountDescriptionDO;
import org.springframework.data.repository.CrudRepository;

public interface SimAccountRepository extends CrudRepository<SimAccountDescriptionDO, String>{

}
