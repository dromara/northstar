package org.dromara.northstar.gateway;

import java.util.List;
import java.util.Optional;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Tick;

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
	
	/**
	 * 获取最近一个TICK数据
	 * @param contract
	 * @return
	 */
	Optional<Tick> lastTick(Contract contract);
}
