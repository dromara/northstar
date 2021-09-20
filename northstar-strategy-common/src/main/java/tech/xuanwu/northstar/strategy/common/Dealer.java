package tech.xuanwu.northstar.strategy.common;

import java.util.Optional;
import java.util.Set;

import tech.xuanwu.northstar.common.model.ContractManager;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 交易策略负责执行信号,以及监听执行结果,并维护交易状态(例如之前的信号是成功执行,还是失败)
 * @author KevinHuangwl
 *
 */
public interface Dealer extends DynamicParamsAware {
	
	/**
	 * 监听行情变动,根据信号下单、撤单或者追单
	 * @param tick
	 * @param riskRules
	 * @param gateway
	 */
	Optional<SubmitOrderReqField> onTick(TickField tick);
	
	/**
	 * 收到信号
	 * @param signal
	 * @param offsetFlag	实操明细
	 */
	void onSignal(Signal signal, OffsetFlagEnum offsetFlag);
	
	/**
	 * 完成交易
	 * @param trade
	 */
	void doneTrade(TradeField trade);
	
	/**
	 * 获取交易策略所绑定的合约列表
	 * @return
	 */
	Set<String> bindedUnifiedSymbols();
	
	/**
	 * 设置合约管理器
	 * @param contractMgr
	 */
	void setContractManager(ContractManager contractMgr);
}
