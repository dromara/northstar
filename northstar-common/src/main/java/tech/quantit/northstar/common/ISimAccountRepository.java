package tech.quantit.northstar.common;

import tech.quantit.northstar.common.model.SimAccountDescription;

public interface ISimAccountRepository {

	/**
	 * 保存账户信息
	 * @param simAccountDescription
	 */
	void save(SimAccountDescription simAccountDescription);
	/**
	 * 查找账户信息
	 * @param accountId
	 * @return
	 */
	SimAccountDescription findById(String accountId);
	/**
	 * 删除账户信息
	 * @param accountId
	 */
	void deleteById(String accountId);
}
