package tech.quantit.northstar.gateway.api.domain.contract;

import tech.quantit.northstar.common.model.Identifier;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
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
	String name();
	
	/**
	 * 唯一标识
	 * @return
	 */
	Identifier identifier();
	
	/**
	 * 种类
	 * @return
	 */
	ProductClassEnum productClass();
	
	/**
	 * 交易所
	 * @return
	 */
	ExchangeEnum exchange();
	
	/**
	 * 转换为合约信息
	 * @return
	 */
	default ContractField mergeToContractField(ContractDefinition contractDef) {
		throw new UnsupportedOperationException();
	}
}
