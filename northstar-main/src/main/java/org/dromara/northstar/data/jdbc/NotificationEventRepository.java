package org.dromara.northstar.data.jdbc;

import org.dromara.northstar.data.jdbc.entity.SubscriptionEventsDO;
import org.springframework.data.repository.CrudRepository;

public interface NotificationEventRepository extends CrudRepository<SubscriptionEventsDO, String>{

}
