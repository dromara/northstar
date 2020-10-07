package tech.xuanwu.northstar.service;

public interface ISimAccountService {

	/**
	 * 模拟入金
	 * @param money
	 */
	void deposit(String gatewayId, int money);
	
	/**
	 * 模拟出金
	 * @param money
	 */
	void withdraw(String gatewayId, int money);
}
