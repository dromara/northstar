package org.dromara.northstar.strategy.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.gateway.api.TradeGateway;

@Deprecated
public class AccountCenter {

	private static AccountCenter instance;
	
	public static AccountCenter getInstance() {
		if(Objects.isNull(instance)) {
			instance = new AccountCenter();
		}
		return instance;
	}

	private Map<TradeGateway, Account> gatewayAccountMap = new HashMap<>();
	
	private AccountCenter() {}
	
	public synchronized void register(TradeGateway gateway) {
		gatewayAccountMap.put(gateway, new Account());
	}
	
	public synchronized Account getAccount(TradeGateway gateway) {
		if(!gatewayAccountMap.containsKey(gateway)) {
			throw new NoSuchElementException("找不到对应的账户：" + gateway.gatewayId());
		}
		return gatewayAccountMap.get(gateway);
	}
}
