package tech.quantit.northstar.data;

import java.util.List;

import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 合约持久化
 * @author KevinHuangwl
 *
 */
public interface IContractRepository {

	/**
	 * 保存合约信息
	 * @param contract
	 */
	void save(ContractField contract, String gatewayType);
	
	/**
	 * 按合约类型查询合约
	 * @param type
	 * @return
	 */
	List<ContractField> findAll(String gatewayType);
	
	
	/**
	 * 查询所有合约
	 * @param type
	 * @return
	 */
	List<ContractField> findAll();
}
