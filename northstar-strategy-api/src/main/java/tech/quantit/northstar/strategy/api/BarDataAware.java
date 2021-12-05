package tech.quantit.northstar.strategy.api;

import com.google.common.eventbus.Subscribe;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * BAR行情组件
 * @author KevinHuangwl
 *
 */
public interface BarDataAware extends Subscribable {
	
	@Subscribe
	void onBar(BarField bar);
}
