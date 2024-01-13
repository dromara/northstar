package org.dromara.northstar.gateway;

import java.util.List;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.core.ContractDefinition;

public interface IMarketCenter extends IContractManager, TickDataAware {
	
	/**
	 * 增加合约定义
	 */
	void addDefinitions(List<ContractDefinition> contractDefs);

	/**
	 * 注册合约
	 * @param ins
	 */
	void addInstrument(Instrument ins);
	
	/**
	 * 注册网关
	 * @param gateway
	 */
	void addGateway(MarketGateway gateway);
	
	/**
	 * 获取网关
	 * @param channelType
	 */
	MarketGateway getGateway(ChannelType channelType);
	
	/**
	 * 加载合约组
	 * @param gatewayId
	 */
	void loadContractGroup(ChannelType channelType);
	
}
