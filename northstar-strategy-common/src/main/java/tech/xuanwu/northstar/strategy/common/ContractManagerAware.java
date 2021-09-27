package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.common.model.ContractManager;

public interface ContractManagerAware {

	/**
	 * 设置合约管理器
	 * @param contractMgr
	 */
	void setContractManager(ContractManager contractMgr);
}
