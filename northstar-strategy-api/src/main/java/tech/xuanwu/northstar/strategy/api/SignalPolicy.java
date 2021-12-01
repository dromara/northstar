package tech.xuanwu.northstar.strategy.api;

public interface SignalPolicy extends TickDataAware, BarDataAware, EventDrivenComponent, StateChangeListener, DynamicParamsAware, ContractBindedAware {

	/**
	 * 策略名称
	 * @return
	 */
	String name();
}
