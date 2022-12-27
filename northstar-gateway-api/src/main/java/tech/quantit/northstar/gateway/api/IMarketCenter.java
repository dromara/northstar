package tech.quantit.northstar.gateway.api;

import tech.quantit.northstar.gateway.api.domain.contract.Instrument;
import tech.quantit.northstar.gateway.api.domain.time.IPeriodHelperFactory;

public interface IMarketCenter extends IContractManager {

	/**
	 * 注册合约
	 * @param ins
	 */
	void addInstrument(Instrument ins, MarketGateway gateway, IPeriodHelperFactory phFactory);
	
	/**
	 * 加载合约组
	 * @param gatewayId
	 */
	void loadContractGroup(String gatewayId);
	
}
