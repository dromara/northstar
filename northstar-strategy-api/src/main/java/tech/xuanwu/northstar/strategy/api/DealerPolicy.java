package tech.xuanwu.northstar.strategy.api;

public interface DealerPolicy extends TickDataAware, EventDrivenComponent, StateChangeListener {

	/**
	 * 策略名称
	 * @return
	 */
	String name();
}
