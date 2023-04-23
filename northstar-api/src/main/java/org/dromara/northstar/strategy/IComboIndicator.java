package org.dromara.northstar.strategy;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.strategy.model.Configuration;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 组合指标接口
 * @author KevinHuangwl
 *
 */
public interface IComboIndicator extends BarDataAware, TickDataAware, MergedBarListener{

	Configuration getConfiguration();
	
	@Override
	default void onMergedBar(BarField bar) {}
	
	@Override
	default void onBar(BarField bar) {}

	@Override
	default void onTick(TickField tick) {}
}
