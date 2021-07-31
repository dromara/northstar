package tech.xuanwu.northstar.gateway.sim.trade;

import tech.xuanwu.northstar.gateway.api.TradeGateway;
import xyz.redtorch.pb.CoreField.TickField;

public interface SimTradeGateway extends TradeGateway{

	/**
	 * 出入金
	 * @param money
	 */
	void moneyIO(int money);
	
	/**
	 * 行情刷新
	 * @param tick
	 */
	void onTick(TickField tick);
	
	/**
	 * 获取账户
	 * @return
	 */
	GwAccountHolder getAccount();
}
