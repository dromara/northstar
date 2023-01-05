package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
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
	default void onBar(BarField bar) {}

	@Override
	default void onTick(TickField tick) {}
}
