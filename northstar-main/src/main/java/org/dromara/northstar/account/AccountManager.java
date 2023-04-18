package org.dromara.northstar.account;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private Map<Identifier, TradeAccount> accountMap = new HashMap<>();
	
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

}
