package tech.quantit.northstar.gateway.api.domain.contract;

import java.util.List;

import tech.quantit.northstar.common.Subscribable;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 抽象合约
 * @author KevinHuangwl
 *
 */
public interface Contract extends Subscribable, Instrument {

	/**
	 * 获取成份合约
	 * @return
	 */
	default List<Contract> memberContracts() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 获取合约信息
	 * @return
	 */
	default ContractField contractField() {
		throw new UnsupportedOperationException();	
	}
	
	/**
	 * 是否可交易
	 * @return
	 */
	default boolean tradable() {
		return false;
	}

}
