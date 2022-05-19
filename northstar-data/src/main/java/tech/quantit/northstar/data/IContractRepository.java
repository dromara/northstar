package tech.quantit.northstar.data;

import java.util.List;

import tech.quantit.northstar.common.constant.GatewayType;
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
	void save(ContractField contract, GatewayType gatewayType);

	/**
	 * 按合约类型查询合约
	 * @param type
	 * @return
	 */
	List<ContractField> findAll(GatewayType gatewayType);

	/**
	 * 查询全部合约
	 * @return
	 */
	List<ContractField> findAll();
}
