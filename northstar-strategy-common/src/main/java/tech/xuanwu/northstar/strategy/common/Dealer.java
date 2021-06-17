package tech.xuanwu.northstar.strategy.common;

import java.util.List;
import java.util.Optional;

import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.strategy.common.model.OrderID;
import xyz.redtorch.pb.CoreField.AccountField;
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
	 * @return 				如果有成交则返回orderId
	 */
	Optional<OrderID> tryDeal(Signal signal, List<RiskControlRule> riskRules, Gateway gateway);
	
	/**
	 * 监听信号执行结果
	 * @param order
	 */
	void onOrder(OrderField order);
	
	/**
	 * 监听信号执行结果
	 * @param trade
	 */
	void onTrade(TradeField trade);
	
	/**
	 * 监听账户变动
	 * @param account
	 */
	void onAccount(AccountField account);
	
	/**
	 * 监听行情变动,根据情况判断撤单或者追单
	 * @param tick
	 * @param riskRules
	 * @param gateway
	 */
	void onTick(TickField tick, List<RiskControlRule> riskRules, Gateway gateway);
	
	/**
	 * 获取交易策略所绑定的合约列表
	 * @return
	 */
	List<String> bindedUnifiedSymbols();
}
