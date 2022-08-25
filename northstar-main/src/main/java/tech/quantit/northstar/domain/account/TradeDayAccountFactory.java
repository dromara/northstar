package tech.quantit.northstar.domain.account;

import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.gateway.api.TradeGateway;

public class TradeDayAccountFactory {
	
	private ContractManager contractMgr;
	private GatewayAndConnectionManager gatewayConnMgr;
	
	public TradeDayAccountFactory(GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
	}
	
	public TradeDayAccount newInstance(String gatewayId) {
		return new TradeDayAccount(gatewayId, (TradeGateway) gatewayConnMgr.getGatewayById(gatewayId), contractMgr);
	}

}
