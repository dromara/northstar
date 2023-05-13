package org.dromara.northstar.gateway;

import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;

public interface IContractManager {

	/**
	 * 根据合约唯一标识获取合约
	 * @param identifier
	 * @return
	 */
	Contract getContract(Identifier identifier);
	
	/**
	 * 根据网关与编码获取合约
	 * @param channelType
	 * @param symbol		可以是symbol或unifiedSymbol 
	 * @return
	 */
	Contract getContract(ChannelType channelType, String symbol);
	
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
