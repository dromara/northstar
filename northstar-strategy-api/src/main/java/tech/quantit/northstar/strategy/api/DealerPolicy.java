package tech.quantit.northstar.strategy.api;

public interface DealerPolicy extends TickDataAware, EventDrivenComponent, StateChangeListener, DynamicParamsAware,
	ContractBindedAware, ModuleNamingAware {

	/**
	 * 策略名称
	 * @return
	 */
	String name();
}
