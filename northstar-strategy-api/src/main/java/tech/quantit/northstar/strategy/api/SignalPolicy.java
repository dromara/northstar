package tech.quantit.northstar.strategy.api;

import java.util.List;

import tech.quantit.northstar.strategy.api.model.TimeSeriesData;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public interface SignalPolicy extends TickDataAware, BarDataAware, EventDrivenComponent, StateChangeListener, DynamicParamsAware, ContractBindedAware {

	/**
	 * 策略名称
	 * @return
	 */
	String name();
	
	/**
	 * 透视引用数据。
	 * @return
	 */
	List<TimeSeriesData> inspectRefData();
	
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
}
