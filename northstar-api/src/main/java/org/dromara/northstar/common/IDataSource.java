package org.dromara.northstar.common;

import java.time.LocalDate;
import java.util.List;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

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
	List<BarField> getMinutelyData(ContractField contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取15分钟K线数据
	 * @param contract
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> getQuarterlyData(ContractField contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取1小时K线数据
	 * @param contract
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> getHourlyData(ContractField contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取日K线数据
	 * @param contract
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> getDailyData(ContractField contract, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取查询范围内的节假日（包括周末）
	 * @param exchange
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<LocalDate> getHolidays(ExchangeEnum exchange, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取交易所全部合约
	 * @param exchange
	 * @return
	 */
	List<ContractField> getAllContracts(ExchangeEnum exchange);
	
	/**
	 * 用户可用交易所
	 * @return
	 */
	List<ExchangeEnum> getUserAvailableExchanges();
}
