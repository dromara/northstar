package org.dromara.northstar.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class TimeSeriesValue implements Comparable<TimeSeriesValue> {
	
	private double value;
	
	private long timestamp;
	
	private boolean unsettled;
	
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
	 * @param unsettled		是否为未确定值（会发生变化）
	 */
	public TimeSeriesValue(double value, long timestamp, boolean unsettled) {
		this.value = value;
		this.timestamp = timestamp;
		this.unsettled = unsettled;
	}
	
	@Override
	public int compareTo(TimeSeriesValue o) {
		return timestamp < o.timestamp ? -1 : 1;
	}
	
}
