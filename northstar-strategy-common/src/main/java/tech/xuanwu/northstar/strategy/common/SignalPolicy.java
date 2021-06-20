package tech.xuanwu.northstar.strategy.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import tech.xuanwu.northstar.strategy.common.event.EventDrivenComponent;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 信号策略只负责通过行情来产生交易信号,至于交易信号的执行,由Dealer来负责
 * @author KevinHuangwl
 *
 */
public interface SignalPolicy extends DynamicParamsAware, EventDrivenComponent {

	/**
	 * 每Tick更新
	 * @param tick		保留Tick数据，因为可能会用到一些Bar没有的信息，例如涨跌停价、日内均价等
	 * @param barData	K线序列数据（已包含Tick更新数据）
	 * @return			信号(不一定有)
	 */
	Optional<Signal> updateTick(TickField tick);
	
	/**
	 * 更新引用Bar数据
	 * @param bar
	 */
	void updateBar(BarField bar);
	
	/**
	 * 获取引用Bar数据
	 * @param unifiedSymbol
	 * @return
	 */
	BarData getRefBarData(String unifiedSymbol);
	
	/**
	 * 获取信号策略所绑定的合约列表
	 * @return
	 */
	Set<String> bindedUnifiedSymbols();
	
	/**
	 * 设置信号策略的引用数据
	 * @param barDataMap
	 */
	void setRefBarData(Map<String, BarData> barDataMap);
	
}
