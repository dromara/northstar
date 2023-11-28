package org.dromara.northstar.strategy;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Tick;

/**
 * 嵌入式策略接口
 * 用于模组策略内部定义子策略
 * @author KevinHuangwl
 *
 */
public interface IEmbededRule extends BarDataAware, TickDataAware {

	/**
	 * 响应TICK数据
	 */
	@Override
	default void onTick(Tick tick) {}

	/**
	 * 响应BAR数据
	 */
	@Override
	default void onBar(Bar bar) {}

}
