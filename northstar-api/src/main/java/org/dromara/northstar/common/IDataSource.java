package org.dromara.northstar.common;

import java.time.LocalDate;
import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.springframework.retry.annotation.Retryable;

/**
 * 历史数据源接口
 */
public interface IDataSource {

	/**
	 * 获取指定周期K线数据
	 * @param contract
	 * @param interval
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@Retryable
	List<Bar> getData(Contract contract, Interval interval, LocalDate startDate, LocalDate endDate);
	
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
