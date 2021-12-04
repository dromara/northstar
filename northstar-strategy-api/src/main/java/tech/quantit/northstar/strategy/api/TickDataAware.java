package tech.quantit.northstar.strategy.api;

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
}
