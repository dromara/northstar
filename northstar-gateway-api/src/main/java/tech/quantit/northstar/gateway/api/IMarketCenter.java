package tech.quantit.northstar.gateway.api;

import tech.quantit.northstar.gateway.api.domain.contract.Instrument;

public interface IMarketCenter extends IContractManager {

	/**
	 * 注册合约
	 * @param ins
	 */
	void addInstrument(Instrument ins);
	
	/**
	 * 网关加载完成
	 * @param gatewayId
	 */
	void onGatewayReady(String gatewayId);
	
}
