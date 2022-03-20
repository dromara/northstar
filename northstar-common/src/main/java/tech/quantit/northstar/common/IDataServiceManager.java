package tech.quantit.northstar.common;

import java.time.LocalDate;
import java.util.List;

import xyz.redtorch.pb.CoreField.BarField;

public interface IDataServiceManager {

	/**
	 * 获取1分钟K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> getMinutelyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取15分钟K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> getQuarterlyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取1小时K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> getHourlyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 获取日K线数据
	 * @param unifiedSymbol
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<BarField> getDailyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate);
	
}
