package tech.xuanwu.northstar.gateway.sim;

import tech.xuanwu.northstar.gateway.api.TradeGateway;
import xyz.redtorch.pb.CoreField.TickField;

public interface SimGateway extends TradeGateway{

	/**
	 * 出入金
	 * @param money
	 */
	void moneyIO(int money);
	
	/**
	 * 行情刷新
	 * @param tick
	 */
	void update(TickField tick);
	
	/**
	 * 保存模拟账户
	 */
	void save();
	
	/**
	 * 载入模拟账户
	 */
	boolean load();
	
	/**
	 * 移除模拟账户
	 */
	void remove();
}
