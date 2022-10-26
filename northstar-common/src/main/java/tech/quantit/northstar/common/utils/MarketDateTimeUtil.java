package tech.quantit.northstar.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import tech.quantit.northstar.common.constant.TickType;

public interface MarketDateTimeUtil {
	
	/**
	 * 根据当前时间获取交易日
	 * @param dateTime
	 * @return
	 */
	LocalDate getTradingDay(LocalDateTime dateTime);
	
	/**
	 * 根据时间判断数据类型
	 * @param symbol
	 * @param time
	 * @return
	 */
	TickType resolveTickType(String symbol, LocalTime time);
	
	/**
	 * 判断为开市时间
	 * @param symbol
	 * @param time
	 * @return
	 */
	boolean isOpeningTime(String symbol, LocalTime time);
}
