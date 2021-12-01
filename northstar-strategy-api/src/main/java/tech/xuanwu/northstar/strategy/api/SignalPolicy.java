package tech.xuanwu.northstar.strategy.api;

public interface SignalPolicy extends TickDataAware, BarDataAware, EventDrivenComponent, StateChangeListener {

	/**
	 * 策略名称
	 * @return
	 */
	String name();
}
