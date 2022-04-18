package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.common.ContractBindedAware;
import tech.quantit.northstar.common.TickDataAware;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

@Deprecated
public interface SignalPolicy extends TickDataAware, BarDataAware, EventDrivenComponent, StateChangeListener,
	DynamicParamsAware, ContractBindedAware, ModuleNamingAware, MailSenderAware {

	/**
	 * 策略名称
	 * @return
	 */
	String name();
	
	/**
	 * 使用TICK数据初始化
	 * @param ticks
	 */
	void initByTick(TickField tick);
	
	/**
	 * 使用BAR数据初始化(1分钟K线数据)
	 * @param bars
	 */
	void initByBar(BarField bars);
	
	/**
	 * 是否已经完成初始化
	 * @return
	 */
	boolean hasDoneInit();
	
	/**
	 * 策略周期（单位：分钟）
	 */
	int periodMins();
	
	/**
	 * 回溯长度
	 */
	int numOfRefData();
}
