package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.common.ContractBindedAware;
import tech.quantit.northstar.common.TickDataAware;

public interface DealerPolicy extends TickDataAware, EventDrivenComponent, StateChangeListener, DynamicParamsAware,
	ContractBindedAware, ModuleNamingAware {

	/**
	 * 策略名称
	 * @return
	 */
	String name();
}
