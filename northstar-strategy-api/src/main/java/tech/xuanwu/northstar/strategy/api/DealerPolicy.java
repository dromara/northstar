package tech.xuanwu.northstar.strategy.api;

public interface DealerPolicy extends TickDataAware, EventDrivenComponent, StateChangeListener, DynamicParamsAware, ContractBindedAware {

	/**
	 * 策略名称
	 * @return
	 */
	String name();
}
