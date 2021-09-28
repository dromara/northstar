package tech.xuanwu.northstar.strategy.common;

import java.util.Optional;

import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 交易策略负责交易管理，例如接收信号、监听止损触发、及实现其他下单逻辑
 * @author KevinHuangwl
 *
 */
public interface Dealer extends DynamicParamsAware, SymbolAware, StatusAware, ContractManagerAware {
	
	/**
	 * 监听行情变动,生成相应的委托单
	 * @param tick
	 * @param riskRules
	 * @param gateway
	 */
	Optional<SubmitOrderReqField> onTick(TickField tick);
	
	/**
	 * 测试止损触发
	 * @param tick
	 * @return
	 */
	Optional<SubmitOrderReqField> tryStopLoss(TickField tick);
	
	/**
	 * 收到信号
	 * @param signal
	 * @param offsetFlag	实操明细
	 */
	void onSignal(Signal signal);
	
	/**
	 * 收到交易回报
	 * @param trade
	 */
	void onTrade(TradeField trade);
	
}
