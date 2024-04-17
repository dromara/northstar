package org.dromara.northstar.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;

public class MarketDataUtils {

	MarketDataUtils(){}
	
	/**
	 * 判断Bar是否为开盘K线
	 * @param bar
	 * @return
	 */
	public static boolean isOpenningBar(Bar bar) {
		TradeTimeDefinition ttd = bar.contract().contractDefinition().tradeTimeDef();
		return ttd.timeSlots().get(0).start().equals(bar.actionTime());
	}
	
	/**
	 * 判断Bar是否为小节开盘K线
	 * @param bar
	 * @return
	 */
	public static boolean isStartingBar(Bar bar) {
		TradeTimeDefinition ttd = bar.contract().contractDefinition().tradeTimeDef();
		for(TimeSlot ts : ttd.timeSlots()) {
			if(ts.start().equals(bar.actionTime())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判断Bar是否为小节收盘K线
	 * @param bar
	 * @return
	 */
	public static boolean isEndingBar(Bar bar) {
		TradeTimeDefinition ttd = bar.contract().contractDefinition().tradeTimeDef();
		for(TimeSlot ts : ttd.timeSlots()) {
			if(ts.end().equals(bar.actionTime())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判断TICK是否为开盘范围内的TICK
	 * 比如要判断一个tick是否为开盘10秒以内的tick，则调用 isOpenningTick(tick, 10000) 来进行判断
	 * @param tick
	 * @param seconds	开盘范围
	 * @return
	 */
	public static boolean isOpenningTick(Tick tick, int seconds) {
		TradeTimeDefinition ttd = tick.contract().contractDefinition().tradeTimeDef();
		LocalTime openTime = ttd.timeSlots().get(0).start();
		LocalTime cutoffTime = openTime.plusSeconds(seconds);
		return tick.actionTime().isBefore(cutoffTime);
	}
	
	/**
	 * 距离整分钟还剩余几秒
	 * @param tick
	 * @return
	 */
	public static int secondsToWholeMin(Tick tick) {
		long t = tick.actionTimestamp();
		return (int) (60 - ((t % 60000) / 1000));
	}
	
	/**
	 * 距离日内收盘还剩余几分钟
	 * @param bar
	 * @return
	 */
	public static int minutesToDayEnd(Bar bar) {
		TradeTimeDefinition ttd = bar.contract().contractDefinition().tradeTimeDef();
		LocalDate tradingDay = bar.tradingDay();
		LocalTime endTime = ttd.timeSlots().get(ttd.timeSlots().size() - 1).end();
		LocalDateTime endDateTime = LocalDateTime.of(tradingDay, endTime);
		return (int) (CommonUtils.localDateTimeToMills(endDateTime) - bar.actionTimestamp()) / 60000;
	}
}
