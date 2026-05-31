package org.dromara.northstar.data.jdbc;

import java.util.List;

import org.dromara.northstar.data.jdbc.entity.HoldingTradesDO;
import org.springframework.data.repository.CrudRepository;

public interface HoldingsRepository extends CrudRepository<HoldingTradesDO, String>{

	List<HoldingTradesDO> findByAccountName(String accountName);
	
	void deleteByAccountName(String accountName);
}
