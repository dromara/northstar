package org.dromara.northstar.gateway.sim.trade;

import org.dromara.northstar.gateway.common.TradeGateway;

public interface SimTradeGateway extends TradeGateway{

	/**
	 * 出入金
	 * @param money
	 * @return 		最新余额
	 */
	int moneyIO(int money);
	
	/**
	 * 销毁
	 */
	void destory();
}
