package tech.xuanwu.northstar.strategy.common.event;

import com.google.common.eventbus.Subscribe;

public interface EventDrivenComponent {

	/**
	 * 监听模组事件
	 * @param event
	 */
	@Subscribe
	void onEvent(ModuleEvent event);
	
	/**
	 * 设置事件总线
	 * @param moduleEventBus
	 */
	void setEventBus(ModuleEventBus moduleEventBus);
}
