package tech.xuanwu.northstar.strategy.common;

import java.util.Optional;

import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 信号策略只负责通过行情来产生交易信号,至于交易信号的执行,由Dealer来负责
 * @author KevinHuangwl
 *
 */
public interface SignalPolicy extends DynamicParamsAware, SymbolAware, StatusAware {
	/**
	 * 触发策略
	 * @param tick
	 */
	Optional<Signal> onTick(TickField tick);

	/**
	 * 每Tick更新
	 * @param tick		保留Tick数据，因为可能会用到一些Bar没有的信息，例如涨跌停价、日内均价等
	 */
	void updateTick(TickField tick);
	
	/**
	 * 更新引用Bar数据
	 * @param bar
	 */
	void updateBar(BarField bar);
	
	/**
	 * 设置Bar数据
	 * @param barData
	 */
	void setBarData(BarData barData);
	
	/**
	 * 获取Bar数据最大回溯长度
	 * @return
	 */
	int getBarDataMaxRefLength();
	
	/**
	 * 获取回溯数据
	 * @param unifiedSymbol
	 * @return
	 */
	BarData getRefBarData(String unifiedSymbol);
	
}
