package org.dromara.northstar.account;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.model.Identifier;
import org.springframework.stereotype.Component;

/**
 * 账户管理器
 * @author KevinHuangwl
 *
 */
@Component
public class AccountManager implements ObjectManager<TradeAccount>{

	private ConcurrentMap<Identifier, TradeAccount> accountMap = new ConcurrentHashMap<>();
	
	@Override
	public void add(TradeAccount account) {
		accountMap.put(Identifier.of(account.accountId()), account);
	}

	@Override
	public void remove(Identifier id) {
		accountMap.remove(id);
	}

	@Override
	public TradeAccount get(Identifier id) {
		return accountMap.get(id);
	}

	@Override
	public boolean contains(Identifier id) {
		return accountMap.containsKey(id);
	}
	
	public List<TradeAccount> allAccounts(){
		return accountMap.values().stream().toList();
	}

	@Override
	public List<TradeAccount> findAll() {
		return allAccounts();
	}

}
