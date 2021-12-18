package xyz.redtorch.gateway.ctp.common;

import java.time.LocalTime;

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
	final long nightMarketClosingTime1 = LocalTime.of(23, 0, 0).toNanoOfDay();
	final long nightMarketClosingTime2 = LocalTime.of(23, 30, 0).toNanoOfDay();
	final long nightMarketClosingTime3 = LocalTime.of(1, 0, 0).toNanoOfDay();
	final long dayMarketClosingTime1 = LocalTime.of(11, 30, 0).toNanoOfDay();
	final long dayMarketClosingTime2 = LocalTime.of(15, 0, 0).toNanoOfDay();
	final long dayMarketClosingTime3 = LocalTime.of(15, 15, 0).toNanoOfDay();
	final long dayMarketEndTime = LocalTime.of(15, 15, 0, 999999).toNanoOfDay();
	
	final long[] closingArr = new long[] {
			nightMarketClosingTime1, 
			nightMarketClosingTime2, 
			nightMarketClosingTime3, 
			dayMarketClosingTime1, 
			dayMarketClosingTime2, 
			dayMarketClosingTime3};
	
	private static final long LESS_THEN_HALF_SEC_IN_NANO = 400000000;
	/**
	 * 根据时间判定Tick类型
	 */
	@Override
	public TickType resolveTickType(LocalTime time) {
		long curTime = time.toNanoOfDay();
		if(curTime < nightMarketEndTime || curTime > dayMarketOpenTime && curTime < dayMarketEndTime || curTime > nightMarketOpenTime) {
			if(aroundAny(closingArr, curTime)) {
				return TickType.CLOSING_TICK;
			}
			return TickType.NORMAL_TICK;
		}
		if(curTime > nightMarketStartTime && curTime < nightMarketOpenTime
				|| curTime > dayMarketStartTime && curTime < dayMarketOpenTime) {
			return TickType.PRE_OPENING_TICK;
		}
		return TickType.NON_OPENING_TICK;
	}
	
	public boolean aroundAny(long[] baseTimeList, long time) {
		for(long baseTime : baseTimeList) {
			if(Math.abs(time - baseTime) <= LESS_THEN_HALF_SEC_IN_NANO) {
				return true;
			}
		}
		return false;
	}

}
