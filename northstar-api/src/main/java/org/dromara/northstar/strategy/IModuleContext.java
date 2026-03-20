package org.dromara.northstar.strategy;

import java.util.List;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;

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
	 * 获取合约绑定的物理账户对象
	 * @param contract
	 * @return
	 */
	IAccount getAccount(Contract contract);
	/**
	 * 预热模组
	 * @param barData
	 */
	void initData(List<Bar> barData);
	/**
	 * 设置运行状态
	 * @param enabled
	 */
	void setEnabled(boolean enabled);
	/**
	 * 是否已完成初始化
	 * @return
	 */
	boolean isReady();
	/**
	 * 获取交易策略
	 * @return
	 */
	TradeStrategy getStrategy();
}
