package tech.xuanwu.northstar.strategy.common;

import java.util.List;

import tech.xuanwu.northstar.gateway.api.Gateway;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 交易策略负责执行信号,以及监听执行结果,并维护交易状态(例如之前的信号是成功执行,还是失败)
 * @author KevinHuangwl
 *
 */
public interface Dealer extends DynamicParamsAware{

	/**
	 * 尝试交易
	 * 如果符合风控规则,则执行信号；否则忽略信号
	 * @param signal
	 * @param riskRules
	 * @param gateway
	 */
	void tryDeal(Signal signal, List<RiskControlRule> riskRules, Gateway gateway);
	
	/**
	 * 监听信号执行结果
	 * @param order
	 */
	void onOrder(OrderField order);
	
	/**
	 * 
	 * @param trade
	 */
	void onTrade(TradeField trade);
	
	void onTick(TickField tick, List<RiskControlRule> riskRules, Gateway gateway);
}
