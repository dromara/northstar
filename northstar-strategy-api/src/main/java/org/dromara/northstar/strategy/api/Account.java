package org.dromara.northstar.strategy.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import xyz.redtorch.pb.CoreField.AccountField;

public class Account {

	private Map<UUID, Double> lockingAmount = new HashMap<>();
	
	private AccountField fields;
	
	public void syncAmount(AccountField accountField) {
		if(Objects.nonNull(fields) && !StringUtils.equals(fields.getAccountId(), accountField.getAccountId())) {
			throw new IllegalStateException("账户ID不一致不能更新");
		}
		fields = accountField;
	}
	
	public synchronized Optional<UUID> trylockAmount(double amount) {
		if(available() < amount) {
			// 如果余额不足，不会锁余额，返回空值
			return Optional.empty();
		}
		
		UUID lockId = UUID.randomUUID();
		lockingAmount.put(lockId, amount);
		return Optional.of(lockId);
	}
	
	private double available() {
		AccountField acc = getAccountField();
		return acc.getAvailable() - sumOfLocking(); 
	}
	
	private double sumOfLocking() {
		return lockingAmount.values().stream().mapToDouble(Double::doubleValue).sum();
	}
	
	public synchronized void unlockAmount(UUID lockId) {
		lockingAmount.remove(lockId);
	}
	
	public AccountField getAccountField() {
		if(Objects.isNull(fields)) {
			throw new IllegalStateException("没有账户的更新信息");
		}
		return fields;
	}
}
