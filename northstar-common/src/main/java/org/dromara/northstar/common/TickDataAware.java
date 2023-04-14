package org.dromara.northstar.common;

import com.google.common.eventbus.Subscribe;

import xyz.redtorch.pb.CoreField.TickField;

/**
 * TICK行情组件
 * @author KevinHuangwl
 *
 */
public interface TickDataAware {

	@Subscribe
	void onTick(TickField tick);
	
	
	default void endOfMarket() {
		throw new UnsupportedOperationException();
	}
}
