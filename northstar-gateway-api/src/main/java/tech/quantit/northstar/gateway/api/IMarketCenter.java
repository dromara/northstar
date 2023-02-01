package tech.quantit.northstar.gateway.api;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.gateway.api.domain.contract.Instrument;

public interface IMarketCenter extends IContractManager, TickDataAware {

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
	 * 停盘收尾处理
	 */
	void endOfMarketTime();
}
