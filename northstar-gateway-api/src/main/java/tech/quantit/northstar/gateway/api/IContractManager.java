package tech.quantit.northstar.gateway.api;

import java.util.List;

import tech.quantit.northstar.common.constant.ChannelType;
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
	 * 根据网关ID获取合约
	 * @param gatewayId
	 * @return
	 */
	List<Contract> getContracts(String gatewayId);
	
	/**
	 * 根据网关渠道获取合约
	 * @param channelType
	 * @return
	 */
	List<Contract> getContracts(ChannelType channelType);
}
