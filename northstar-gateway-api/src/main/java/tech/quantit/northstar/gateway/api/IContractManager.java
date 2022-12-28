package tech.quantit.northstar.gateway.api;

import java.util.List;

import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;

public interface IContractManager {

	/**
	 * 根据合约唯一标识获取合约
	 * @param identifier
	 * @return
	 */
	Contract getContract(Identifier identifier);
	
	/**
	 * 根据网关与编码获取合约
	 * @param gatewayId
	 * @param symbol
	 * @return
	 */
	Contract getContract(String gatewayId, String symbol);
	
	/**
	 * 获取网关全部合约
	 * @param gatewayId
	 * @return
	 */
	List<Contract> getContracts(String gatewayId);
}
