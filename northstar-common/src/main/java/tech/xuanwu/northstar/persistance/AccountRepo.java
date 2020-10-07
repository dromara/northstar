package tech.xuanwu.northstar.persistance;

import java.util.List;

import tech.xuanwu.northstar.persistance.po.Account;

public interface AccountRepo{

	Account findByGatewayId(String gatewayId);
	
	void save(Account account);
	
	List<Account> findAll();
}
