package tech.quantit.northstar.data;

import java.util.List;

import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 合约持久化
 * @author KevinHuangwl
 *
 */
public interface IContractRepository {

	/**
	 * 批量保存合约信息
	 * @param contracts
	 */
	void batchSave(List<ContractField> contracts);
	
	/**
	 * 保存合约信息
	 * @param contract
	 */
	void save(ContractField contract);
	
	/**
	 * 按合约类型查询合约
	 * @param type
	 * @return
	 */
	List<ContractField> findAllByType(ProductClassEnum type);
}
