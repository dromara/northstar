package tech.quantit.northstar.strategy.api;

import java.util.Map;

import tech.quantit.northstar.common.ContractBindedAware;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.strategy.api.model.TimeSeriesValue;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public interface SignalPolicy extends TickDataAware, BarDataAware, EventDrivenComponent, StateChangeListener,
	DynamicParamsAware, ContractBindedAware, ModuleNamingAware {

	/**
	 * 策略名称
	 * @return
	 */
	String name();
	
	/**
	 * 透视引用数据。
	 * @return
	 */
	Map<String, TimeSeriesValue[]> inspectRefData();
	
	/**
	 * 使用TICK数据初始化
	 * @param ticks
	 */
	void initByTick(Iterable<TickField> ticks);
	
	/**
	 * 使用BAR数据初始化(1分钟K线数据)
	 * @param bars
	 */
	void initByBar(Iterable<BarField> bars);
	
	/**
	 * 策略周期（单位：分钟）
	 */
	int periodMins();
	
	/**
	 * 回溯长度
	 */
	int numOfRefData();
}
