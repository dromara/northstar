package org.dromara.northstar.data.jdbc;

import java.util.List;

import org.dromara.northstar.data.jdbc.entity.OverlayDO;
import org.springframework.data.repository.CrudRepository;

public interface OverlayRepository extends CrudRepository<OverlayDO, String>{

	List<OverlayDO> findByContractId(String contractId);
	
	void deleteByContractId(String contractId);
}
