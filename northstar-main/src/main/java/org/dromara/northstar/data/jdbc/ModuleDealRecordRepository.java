package org.dromara.northstar.data.jdbc;

import java.util.List;

import org.dromara.northstar.data.jdbc.entity.ModuleDealRecordDO;
import org.springframework.data.repository.CrudRepository;

public interface ModuleDealRecordRepository extends CrudRepository<ModuleDealRecordDO, Integer>{

	List<ModuleDealRecordDO> findByModuleName(String moduleName);
	
	void deleteByModuleName(String moduleName);
}
