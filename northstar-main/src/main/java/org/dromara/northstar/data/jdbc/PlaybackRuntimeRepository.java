package org.dromara.northstar.data.jdbc;

import org.dromara.northstar.data.jdbc.model.PlaybackRuntimeDescriptionDO;
import org.springframework.data.repository.CrudRepository;

public interface PlaybackRuntimeRepository extends CrudRepository<PlaybackRuntimeDescriptionDO, String>{

}
