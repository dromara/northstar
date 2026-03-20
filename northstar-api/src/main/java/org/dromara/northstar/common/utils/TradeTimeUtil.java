package org.dromara.northstar.common.utils;

import java.time.LocalTime;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.dromara.northstar.common.model.core.TradeTimeDefinition;

public class TradeTimeUtil {
	
	private static final int SEC_PER_MIN = 60;
	private static final int MINS_OF_DAY = 1440;
	
	private BitSet tradeTimeBitmap = new BitSet(MINS_OF_DAY);
	
	private Set<LocalTime> endsOfSection = new HashSet<>();

	public TradeTimeUtil(TradeTimeDefinition ttd) {
		ttd.timeSlots().forEach(ts -> {
			LocalTime start = ts.start();
			LocalTime end = ts.end();
			if(end.isBefore(start)) {
				tradeTimeBitmap.set(start.plusMinutes(1).toSecondOfDay() / SEC_PER_MIN, MINS_OF_DAY);
				tradeTimeBitmap.set(0, end.toSecondOfDay() / SEC_PER_MIN);
			} else {				
				tradeTimeBitmap.set(start.plusMinutes(1).toSecondOfDay() / SEC_PER_MIN, end.toSecondOfDay() / SEC_PER_MIN);
			}
			endsOfSection.add(end);
		});
	}
	
	public boolean withinTradeTime(LocalTime t) {
		LocalTime checkTime = t.withSecond(0).withNano(0).plusMinutes(1);
		return endsOfSection.contains(t) || tradeTimeBitmap.get(checkTime.toSecondOfDay() / SEC_PER_MIN);
	}
}
