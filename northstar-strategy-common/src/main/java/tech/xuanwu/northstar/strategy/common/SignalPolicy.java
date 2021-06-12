package tech.xuanwu.northstar.strategy.common;

import java.util.Optional;

import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import xyz.redtorch.pb.CoreField.TickField;

public interface SignalPolicy extends DynamicParamsAware{

	/**
	 * 每Tick更新
	 * @param tick		保留Tick数据，因为可能会用到一些Bar没有的信息，例如涨跌停价、日内均价等
	 * @param barData	K线序列数据（已包含Tick更新数据）
	 * @return			信号(不一定有)
	 */
	Optional<Signal> updateTick(TickField tick, BarData barData);
}
