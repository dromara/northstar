package tech.xuanwu.northstar.strategy.common;

import java.util.Optional;
import java.util.Set;

import tech.xuanwu.northstar.strategy.common.event.EventDrivenComponent;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 交易策略负责执行信号,以及监听执行结果,并维护交易状态(例如之前的信号是成功执行,还是失败)
 * @author KevinHuangwl
 *
 */
public interface Dealer extends DynamicParamsAware, EventDrivenComponent {
	
	/**
	 * 监听行情变动,根据信号下单、撤单或者追单
	 * @param tick
	 * @param riskRules
	 * @param gateway
	 */
	Optional<SubmitOrderReqField> onTick(TickField tick);
	
	/**
	 * 获取交易策略所绑定的合约列表
	 * @return
	 */
	Set<String> bindedUnifiedSymbols();
}
