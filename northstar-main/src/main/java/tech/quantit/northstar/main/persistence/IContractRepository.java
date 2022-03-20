package tech.quantit.northstar.main.persistence;

import java.util.List;

import tech.quantit.northstar.main.persistence.po.ContractPO;

public interface IContractRepository {

	/**
	 * 批量保存合约信息
	 * @param contracts
	 */
	void batchSaveContracts(List<ContractPO> contracts);
	
	/**
	 * 保存合约信息
	 * @param contract
	 */
	void saveContract(ContractPO contract);
	
	/**
	 * 查询有效合约列表
	 * @return
	 */
	List<ContractPO> getAvailableContracts();
}
