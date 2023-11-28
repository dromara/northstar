package org.dromara.northstar.common;

import org.dromara.northstar.common.model.core.Tick;

/**
 * TICK行情组件
 * @author KevinHuangwl
 *
 */
public interface TickDataAware {

	
	void onTick(Tick tick);
	
	
	default void endOfMarket() {
		throw new UnsupportedOperationException();
	}
}
