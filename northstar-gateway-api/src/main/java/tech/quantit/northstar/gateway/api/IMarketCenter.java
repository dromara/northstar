package tech.quantit.northstar.gateway.api;

import tech.quantit.northstar.gateway.api.domain.contract.Instrument;

public interface IMarketCenter extends IContractManager {

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
	 * 加载合约组
	 * @param gatewayId
	 */
	void loadContractGroup(String gatewayId);
	
	/**
	 * 停盘收尾处理
	 */
	void endOfMarketTime();
}
