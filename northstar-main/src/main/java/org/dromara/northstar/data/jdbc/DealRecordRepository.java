package org.dromara.northstar.data.jdbc;

import java.util.List;

import org.dromara.northstar.data.jdbc.entity.DealRecordDO;
import org.springframework.data.repository.CrudRepository;

public interface DealRecordRepository extends CrudRepository<DealRecordDO, Integer>{

	List<DealRecordDO> findByAccountName(String accountName);
	
	void deleteByAccountName(String accountName);
}
