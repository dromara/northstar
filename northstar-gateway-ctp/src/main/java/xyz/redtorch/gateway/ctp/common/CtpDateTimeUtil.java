package xyz.redtorch.gateway.ctp.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Pattern;

import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.utils.MarketDateTimeUtil;

public class CtpDateTimeUtil implements MarketDateTimeUtil{

	@Override
	public LocalDate getTradingDay(LocalDateTime dateTime) {
		if(dateTime.getDayOfWeek() == DayOfWeek.FRIDAY && dateTime.toLocalTime().isAfter(LocalTime.of(16, 0))) {
			return dateTime.plusHours(56).toLocalDate();
		}
		if(dateTime.toLocalTime().isAfter(LocalTime.of(16, 0))) {
			return dateTime.plusHours(4).toLocalDate();
		}
		
		return dateTime.toLocalDate();
	}
	
	final LocalTime nightMarketStartTime = LocalTime.of(20, 58, 59, 999999999);
	final LocalTime nightMarketOpenTime = LocalTime.of(20, 59, 59, 999999999);
	final LocalTime nightMarketEndTime = LocalTime.of(2, 30, 0, 999999);
	final LocalTime dayMarketStartTime = LocalTime.of(8, 58, 59, 999999999);
	final LocalTime dayMarketOpenTime = LocalTime.of(8, 59, 59, 999999999);
	final LocalTime dayMarketEndTime = LocalTime.of(15, 0, 0, 999999);
	final LocalTime dayMarketOpenTime2 = LocalTime.of(9, 29, 59, 999999999);
	final LocalTime dayMarketEndTime2 = LocalTime.of(15, 15, 0, 999999);
	
	private static final long LESS_THEN_HALF_SEC_IN_NANO = 1000000000L;
	
	private Pattern bondPtn = Pattern.compile("^T[SF]?[0-9]{4}$");
	
	/**
	 * 根据时间判定Tick类型
	 */
	@Override
	public TickType resolveTickType(String symbol, LocalTime time) {
		boolean isBond = bondPtn.matcher(symbol).matches();
		long curTime = time.toNanoOfDay();
		
		if(isBond) {
			if(curTime > toNanoOfDay(dayMarketOpenTime2) && curTime < toNanoOfDay(dayMarketEndTime2)) {
					return closeToTime(toNanoOfDay(dayMarketEndTime2), curTime) ? TickType.CLOSING_TICK : TickType.NORMAL_TICK;
			}
			if(curTime > toNanoOfDay(dayMarketStartTime) && curTime < toNanoOfDay(dayMarketOpenTime2)) {
				return TickType.PRE_OPENING_TICK;
			}
		} else {
			if(curTime < toNanoOfDay(nightMarketEndTime) || curTime > toNanoOfDay(nightMarketOpenTime) 
					|| (curTime > toNanoOfDay(dayMarketOpenTime) && curTime < toNanoOfDay(dayMarketEndTime))) {
				return closeToTime(toNanoOfDay(dayMarketEndTime), curTime) ? TickType.CLOSING_TICK : TickType.NORMAL_TICK;
			}
			if(curTime > toNanoOfDay(nightMarketStartTime) && curTime < toNanoOfDay(nightMarketOpenTime)
					|| curTime > toNanoOfDay(dayMarketStartTime) && curTime < toNanoOfDay(dayMarketOpenTime)) {
				return TickType.PRE_OPENING_TICK;
			}
		}
		return TickType.NON_OPENING_TICK;
	}
	
	private long toNanoOfDay(LocalTime time) {
		return time.toNanoOfDay();
	}
	
	private boolean closeToTime(long baseTime, long time) {
		return time < baseTime && baseTime - time < LESS_THEN_HALF_SEC_IN_NANO;
	}

	@Override
	public boolean isOpeningTime(String symbol, LocalTime time) {
		boolean isBond = bondPtn.matcher(symbol).matches();
		boolean nightTime = time.isAfter(nightMarketStartTime) || time.isBefore(nightMarketEndTime);
		boolean dayTime = time.isAfter(dayMarketStartTime) && time.isBefore(isBond ? dayMarketEndTime2 : dayMarketEndTime);
		return dayTime || nightTime;
	}
}
