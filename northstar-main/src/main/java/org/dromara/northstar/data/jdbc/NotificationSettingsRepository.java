package org.dromara.northstar.data.jdbc;

import org.dromara.northstar.data.jdbc.entity.NotificationSettingsDO;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationSettingsRepository extends CrudRepository<NotificationSettingsDO, String>{

}
