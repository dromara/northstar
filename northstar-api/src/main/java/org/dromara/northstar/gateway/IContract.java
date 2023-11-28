package org.dromara.northstar.gateway;

import java.util.List;

import org.dromara.northstar.common.Subscribable;

import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 抽象合约
 * @author KevinHuangwl
 *
 */
public interface IContract extends Subscribable, Instrument {
	
	/**
	 * 网关ID
	 * @return
	 */
	String gatewayId();
	
	/**
	 * 获取成份合约
	 * @return
	 */
	default List<IContract> memberContracts() {
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
