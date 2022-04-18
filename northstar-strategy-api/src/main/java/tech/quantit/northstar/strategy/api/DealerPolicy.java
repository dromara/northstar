package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.common.ContractBindedAware;
import tech.quantit.northstar.common.TickDataAware;

@Deprecated
public interface DealerPolicy extends TickDataAware, EventDrivenComponent, StateChangeListener, DynamicParamsAware,
	ContractBindedAware, ModuleNamingAware, MailSenderAware {

	/**
	 * 策略名称
	 * @return
	 */
	String name();
}
