package org.dromara.northstar.data.jdbc;

import java.util.List;

import org.dromara.northstar.data.jdbc.entity.AccountDO;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<AccountDO, String> {

	List<AccountDO> findByAccountName(String accountName);
	
	void deleteByAccountName(String accountName);
}
