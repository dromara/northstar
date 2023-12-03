package org.dromara.northstar.data;

import java.time.LocalDate;
import java.util.List;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;

/**
 * 行情数据持久化
 * @author KevinHuangwl
 *
 */
public interface IMarketDataRepository {

	/**
	 * 保存数据
	 * @param bar
	 */
	void insert(Bar bar);
	
	/**
	 * 加载历史行情分钟K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<Bar> loadBars(Contract contract, LocalDate startDate, LocalDate endDate);
	
	
	/**
	 * 加载历史行情日K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<Bar> loadDailyBars(Contract contract, LocalDate startDate, LocalDate endDate);
	
}
