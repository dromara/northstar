package org.dromara.northstar.common;

import java.time.LocalDate;
import java.util.List;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Retryable;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

/**
 * 历史数据源接口
 */
public interface IDataSource {

	/**
	 * 获取1分钟K线数据
	 * @param contract
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", key = "'Minutely_' + #contract.unifiedSymbol() + '_' + #startDate + '_' + #endDate")
	List<Bar> getMinutelyData(Contract contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取15分钟K线数据
	 * @param contract
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", key = "'Quarterly_' + #contract.unifiedSymbol() + '_' + #startDate + '_' + #endDate")
	List<Bar> getQuarterlyData(Contract contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取1小时K线数据
	 * @param contract
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", key = "'Hourly_' + #contract.unifiedSymbol() + '_' + #startDate + '_' + #endDate")
	List<Bar> getHourlyData(Contract contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取日K线数据
	 * @param contract
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", key = "'Daily_' + #contract.unifiedSymbol() + '_' + #startDate + '_' + #endDate")
	List<Bar> getDailyData(Contract contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取查询范围内的节假日（包括周末）
	 * @param exchange
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Retryable
	List<LocalDate> getHolidays(ExchangeEnum exchange, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取交易所全部合约
	 * @param exchange
	 * @return
	 */
	@Retryable
	List<Contract> getAllContracts(ExchangeEnum exchange);
	
	/**
	 * 用户可用交易所
	 * @return
	 */
	@Retryable
	List<ExchangeEnum> getUserAvailableExchanges();
}
