package tech.xuanwu.northstar.strategy.common;

import xyz.redtorch.pb.CoreField.AccountField;

/**
 * 用于计算模组与账户的关联信息
 * 例如模组占账户的比例会影响模组是否有足够的资金开仓
 * @author KevinHuangwl
 *
 */
public interface ModuleAccount {
	
	/**
	 * 更新账户
	 * @param account
	 */
	void updateAccount(AccountField account);
	
	/**
	 * 获取账户信息
	 * @return
	 */
	AccountField getAccount();
	
	/**
	 * 获取账户分配比例(单位%)
	 * @return
	 */
	int getAccountShareInPercentage();
	
	/**
	 * 获取账户可用部分资金
	 * @return
	 */
	double getAccountAvailable();
}
