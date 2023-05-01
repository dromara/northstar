package org.dromara.northstar.data.jdbc;

import org.dromara.northstar.data.jdbc.model.MailConfigDescriptionDO;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MailConfigDescriptionRepository extends CrudRepository<MailConfigDescriptionDO, String>{

}
