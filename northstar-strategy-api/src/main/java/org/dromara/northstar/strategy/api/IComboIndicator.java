package org.dromara.northstar.strategy.api;

import org.dromara.northstar.strategy.api.indicator.Indicator;

import tech.quantit.northstar.common.TickDataAware;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 组合指标接口
 * @author KevinHuangwl
 *
 */
public interface IComboIndicator extends BarDataAware, TickDataAware, MergedBarListener{

	Indicator.Configuration getConfiguration();
	
	@Override
	default void onMergedBar(BarField bar) {}
	
	@Override
	default void onBar(BarField bar) {}

	@Override
	default void onTick(TickField tick) {}
}
