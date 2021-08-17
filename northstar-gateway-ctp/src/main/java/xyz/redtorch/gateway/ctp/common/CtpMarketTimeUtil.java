package xyz.redtorch.gateway.ctp.common;

import java.time.LocalTime;

import tech.xuanwu.northstar.common.constant.TickType;
import tech.xuanwu.northstar.common.utils.MarketTimeUtil;

/**
 * CTP市场行情解析
 * @author KevinHuangwl
 *
 */
public class CtpMarketTimeUtil implements MarketTimeUtil{
	
	LocalTime nightMarketStartTime = LocalTime.of(20, 58, 59, 999999999);
	LocalTime nightMarketOpenTime = LocalTime.of(21, 0, 1);
	LocalTime nightMarketEndTime = LocalTime.of(2, 30, 0, 999999);
	LocalTime dayMarketStartTime = LocalTime.of(8, 58, 59, 999999999);
	LocalTime dayMarketOpenTime = LocalTime.of(9, 0, 1);
	LocalTime dayMarketClosingTime1 = LocalTime.of(15, 0, 0);
	LocalTime dayMarketClosingTime2 = LocalTime.of(15, 15, 0);
	LocalTime dayMarketEndTime = LocalTime.of(15, 15, 0, 999999);
	
	long LESS_THEN_HALF_SEC_IN_NANO = 400000000;
	/**
	 * 根据时间判定Tick类型
	 */
	@Override
	public TickType resolveTickType(LocalTime time) {
		
		if(time.isAfter(nightMarketEndTime) && time.isBefore(dayMarketStartTime)
				|| time.isAfter(dayMarketEndTime) && time.isBefore(nightMarketStartTime)) {
			return TickType.NON_OPENING_TICK;
		}
		if(time.isAfter(nightMarketStartTime) && time.isBefore(nightMarketOpenTime)
				|| time.isAfter(dayMarketStartTime) && time.isBefore(dayMarketOpenTime)) {
			return TickType.PRE_OPENING_TICK;
		}
		if(time.getSecond() == 0 && time.getNano() == 0) {
			return TickType.END_OF_MIN_TICK;
		}
		if(Math.abs(dayMarketClosingTime1.toNanoOfDay() - time.toNanoOfDay()) < LESS_THEN_HALF_SEC_IN_NANO
				|| Math.abs(dayMarketClosingTime2.toNanoOfDay() - time.toNanoOfDay()) < LESS_THEN_HALF_SEC_IN_NANO) {
			return TickType.CLOSING_TICK;
		}
		return TickType.NORMAL_TICK;
	}

}
