package org.dromara.northstar.gateway.sim.trade;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.gateway.TradeGateway;

public interface SimTradeGateway extends TradeGateway, TickDataAware{

	/**
	 * 出入金
	 * @param money
	 * @return 		最新余额
	 */
	int moneyIO(int money);
	
}
