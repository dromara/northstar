package tech.quantit.northstar.strategy.api.utils.time;

import java.time.LocalTime;

/**
 * 时间段工具类
 * @author KevinHuangwl
 *
 */
public class TimeRange {
	
	private LocalTime startTime;
	private LocalTime endTime;

	public TimeRange(LocalTime startTime, LocalTime endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	/**
	 * 是否在时段内
	 * @return
	 */
	public boolean isWithinPeriod(LocalTime time) {
		return time.isAfter(startTime) && time.isBefore(endTime);
	}
}
