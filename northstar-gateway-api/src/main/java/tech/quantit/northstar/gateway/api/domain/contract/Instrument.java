package tech.quantit.northstar.gateway.api.domain.contract;

import tech.quantit.northstar.common.model.Identifier;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * （可交易的）投资品种
 * @author KevinHuangwl
 *
 */
public interface Instrument {

	/**
	 * 名称
	 * @return
	 */
	String symbol();
	
	/**
	 * 唯一标识
	 * @return
	 */
	Identifier indentifier();
	
	/**
	 * 转换为合约信息
	 * @return
	 */
	ContractField mergeToContractField(ContractDefinition contractDef);
}
