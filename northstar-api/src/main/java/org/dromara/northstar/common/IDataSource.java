package org.dromara.northstar.common;

import java.time.LocalDate;
import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Retryable;

/**
 * 历史数据源接口
 */
public interface IDataSource {

	/**
	 * 获取1分钟K线数据
	 * @param contract
	 * @param startDate		开始日（交易日）
	 * @param endDate		结束日（交易日）
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", keyGenerator = "barCacheKeyGenerator")
	List<Bar> getMinutelyData(Contract contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取15分钟K线数据
	 * @param contract
	 * @param startDate		开始日（交易日）
	 * @param endDate		结束日（交易日）
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", keyGenerator = "barCacheKeyGenerator")
	List<Bar> getQuarterlyData(Contract contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取1小时K线数据
	 * @param contract
	 * @param startDate		开始日（交易日）
	 * @param endDate		结束日（交易日）
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", keyGenerator = "barCacheKeyGenerator")
	List<Bar> getHourlyData(Contract contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取日K线数据
	 * @param contract
	 * @param startDate		开始日（交易日）
	 * @param endDate		结束日（交易日）
	 * @return
	 */
	@Retryable
	@Cacheable(cacheNames = "bars", keyGenerator = "barCacheKeyGenerator")
	List<Bar> getDailyData(Contract contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取查询范围内的节假日（包括周末）
	 * @param channelType
	 * @param startDate		开始日（交易日）
	 * @param endDate		结束日（交易日）
	 * @return
	 */
	@Retryable
	List<LocalDate> getHolidays(ChannelType channelType, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取交易所全部合约
	 * @param exchange
	 * @return
	 */
	@Retryable
	List<Contract> getAllContracts();
	
}
