package tech.quantit.northstar.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class TimeSeriesValue implements Comparable<TimeSeriesValue> {
	
	private double value;
	
	private long timestamp;
	
	private boolean isSettled = true;
	
	/**
	 * @param value			时序值
	 * @param timestamp		值对应的时间
	 */
	public TimeSeriesValue(double value, long timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}

	/**
	 * @param value			时序值
	 * @param timestamp		值对应的时间
	 * @param isSettled		是否为确定值（不会再发生变化）
	 */
	public TimeSeriesValue(double value, long timestamp, boolean isSettled) {
		this.value = value;
		this.timestamp = timestamp;
		this.isSettled = isSettled;
	}
	
	@Override
	public int compareTo(TimeSeriesValue o) {
		return timestamp < o.timestamp ? -1 : 1;
	}
	
}
