package tech.xuanwu.northstar.strategy.common;

import xyz.redtorch.pb.CoreField.AccountField;

/**
 * 用于计算模组与账户的关联信息
 * 例如模组占账户的比例会影响模组是否有足够的资金开仓
 * @author KevinHuangwl
 *
 */
public class GenericModuleAccount implements ModuleAccount{
	
	private final int accountShareInPercentage;
	
	private volatile AccountField account;

	public GenericModuleAccount(int shareInPercentage) {
		accountShareInPercentage = shareInPercentage;
	}

	@Override
	public void updateAccount(AccountField account) {
		this.account = account;
	}

	@Override
	public AccountField getAccount() {
		return account;
	}

	@Override
	public int getAccountShareInPercentage() {
		return accountShareInPercentage;
	}

	@Override
	public double getAccountAvailable() {
		return Math.min(account.getBalance() * accountShareInPercentage / 100.0, account.getAvailable());
	}
}
