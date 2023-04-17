package org.dromara.northstar.account;

import org.dromara.northstar.gateway.api.IContractManager;
import org.dromara.northstar.gateway.api.TradeGateway;

@Deprecated
public class TradeDayAccountFactory {
	
	private IContractManager contractMgr;
	private GatewayAndConnectionManager gatewayConnMgr;
	
	public TradeDayAccountFactory(GatewayAndConnectionManager gatewayConnMgr, IContractManager contractMgr) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
	}
	
	public TradeDayAccount newInstance(String gatewayId) {
		return new TradeDayAccount(gatewayId, (TradeGateway) gatewayConnMgr.getGatewayById(gatewayId), contractMgr);
	}

}
