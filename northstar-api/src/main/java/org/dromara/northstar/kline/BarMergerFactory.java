package org.dromara.northstar.kline;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Contract;

public class BarMergerFactory {
	
	private BarMergerFactory() {}

	public static BarMerger newInstance(Interval interval, Contract contract) {
		if(interval == Interval.WK1) {
			return new WeeklyBarMerger(contract);
		} else if(interval == Interval.D1) {
			return new DailyBarMerger(contract);
		}
		
		return new BarMerger(interval, contract);
	}
}
