package xyz.redtorch.gateway.ctp.common;

import java.time.LocalTime;
import java.util.regex.Pattern;

import tech.quantit.northstar.common.constant.TickType;
import tech.quantit.northstar.common.utils.MarketTimeUtil;

/**
 * CTP市场行情解析
 * @author KevinHuangwl
 *
 */
public class CtpMarketTimeUtil implements MarketTimeUtil{
	
	final long nightMarketStartTime = LocalTime.of(20, 58, 59, 999999999).toNanoOfDay();
	final long nightMarketOpenTime = LocalTime.of(20, 59, 59, 999999999).toNanoOfDay();
	final long nightMarketEndTime = LocalTime.of(2, 30, 0, 999999).toNanoOfDay();
	final long dayMarketStartTime = LocalTime.of(8, 58, 59, 999999999).toNanoOfDay();
	final long dayMarketOpenTime = LocalTime.of(8, 59, 59, 999999999).toNanoOfDay();
	final long dayMarketEndTime = LocalTime.of(15, 0, 0, 999999).toNanoOfDay();
	final long dayMarketOpenTime2 = LocalTime.of(9, 29, 59, 999999999).toNanoOfDay();
	final long dayMarketEndTime2 = LocalTime.of(15, 15, 0, 999999).toNanoOfDay();
	
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
			if(curTime > dayMarketOpenTime2 && curTime < dayMarketEndTime2) {
					return closeToTime(dayMarketEndTime2, curTime) ? TickType.CLOSING_TICK : TickType.NORMAL_TICK;
			}
			if(curTime > dayMarketStartTime && curTime < dayMarketOpenTime2) {
				return TickType.PRE_OPENING_TICK;
			}
		} else {
			if(curTime < nightMarketEndTime || (curTime > dayMarketOpenTime && curTime < dayMarketEndTime) || curTime > nightMarketOpenTime) {
				return closeToTime(dayMarketEndTime, curTime) ? TickType.CLOSING_TICK : TickType.NORMAL_TICK;
			}
			if(curTime > nightMarketStartTime && curTime < nightMarketOpenTime
					|| curTime > dayMarketStartTime && curTime < dayMarketOpenTime) {
				return TickType.PRE_OPENING_TICK;
			}
		}
		return TickType.NON_OPENING_TICK;
	}
	
	public boolean closeToTime(long baseTime, long time) {
		return time < baseTime && baseTime - time < LESS_THEN_HALF_SEC_IN_NANO;
	}
}
