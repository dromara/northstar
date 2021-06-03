package tech.xuanwu.northstar.strategy.common;

import xyz.redtorch.pb.CoreField.TickField;

public interface SignalPolicy extends NamedComponent, DynamicParamsAware{

	/**
	 * 每Tick更新
	 * @param tick		保留Tick数据，因为可能会用到一些Bar没有的信息，例如涨跌停价、日内均价等
	 * @param barData	K线序列数据（已包含Tick更新数据）
	 */
	void updateTick(TickField tick, BarData barData);
}
