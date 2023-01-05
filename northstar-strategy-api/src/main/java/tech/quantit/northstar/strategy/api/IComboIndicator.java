package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.strategy.api.indicator.Indicator;

/**
 * 组合指标接口
 * @author KevinHuangwl
 *
 */
public interface IComboIndicator extends BarDataAware, TickDataAware, MergedBarListener{

	Indicator.Configuration getConfiguration();
}
