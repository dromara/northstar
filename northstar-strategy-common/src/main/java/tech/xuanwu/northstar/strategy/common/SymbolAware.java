package tech.xuanwu.northstar.strategy.common;

import java.util.Set;

public interface SymbolAware {

	/**
	 * 获取信号策略所绑定的合约列表
	 * @return
	 */
	Set<String> bindedUnifiedSymbols();
}
