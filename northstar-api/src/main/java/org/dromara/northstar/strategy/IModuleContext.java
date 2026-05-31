package org.dromara.northstar.strategy;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;

public interface IModuleContext extends IModuleStrategyContext, MergedBarListener, TickDataAware, BarDataAware, TransactionAware {
	/**
	 * 获取模组配置
	 * @return
	 */
	ModuleDescription getModuleDescription();
	/**
	 * 获取模组运行时
	 * @return
	 */
	ModuleRuntimeDescription getRuntimeDescription();
	/**
	 * 获取模组图表
	 * @return
	 */
	ModuleChartDescription getChartDescription();
	/**
	 * 设置运行状态
	 * @param enabled
	 */
	void setEnabled(boolean enabled);
	/**
	 * 获取交易策略
	 * @return
	 */
	TradeStrategy getStrategy();
}
