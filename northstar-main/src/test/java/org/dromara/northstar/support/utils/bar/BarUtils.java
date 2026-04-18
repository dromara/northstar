package org.dromara.northstar.support.utils.bar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Random;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;

import org.dromara.northstar.common.constant.Interval;

public class BarUtils {
	
	private Bar lastBar;
	
	private int priceSeed;
	
	private Contract contract;
	
	private Interval interval;
	
	private Random rand;
	
	public BarUtils(int priceSeed, Contract contract, Interval interval) {
		this.priceSeed = priceSeed;
		this.contract = contract;
		this.interval = interval;
		this.rand = new Random(priceSeed);
	}
	
	public Bar genBar(LocalDate date, LocalTime time){
		double open = lastBar == null ? priceSeed : lastBar.closePrice();
		double high = open * (1 + rand.nextDouble(0.03));
		double low = open * (1 - rand.nextDouble(0.03));
		double close = rand.nextDouble(low, high);
		
		double oiDelta = rand.nextLong(1000);
		double volDelta = rand.nextInt(1000);
		double toDelta = rand.nextInt(100000);
		
		double oi = (lastBar == null ? 0 : lastBar.openInterest()) + oiDelta;
		double vol = (lastBar == null ? 0 : lastBar.volume()) + volDelta;
		double to = (lastBar == null ? 0 : lastBar.turnover()) + toDelta;
    	return Bar.builder()
    			.contract(contract)
				.actionDay(date)
				.actionTime(time)
				.actionTimestamp(LocalDateTime.of(date, time).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.openPrice(open)
				.closePrice(close)
				.highPrice(high)
				.lowPrice(low)
				.openInterest((long) oi)
				.openInterestDelta((int) oiDelta)
				.volume((long) vol)
				.volumeDelta((int) volDelta)
				.turnover(to)
				.turnoverDelta(toDelta)
				.interval(interval)
				.build();
	}
}
