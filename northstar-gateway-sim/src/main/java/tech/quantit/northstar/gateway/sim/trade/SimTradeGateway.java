package tech.quantit.northstar.gateway.sim.trade;

import tech.quantit.northstar.gateway.api.TradeGateway;

public interface SimTradeGateway extends TradeGateway{

	/**
	 * 出入金
	 * @param money
	 * @return 		最新余额
	 */
	int moneyIO(int money);
	
}
