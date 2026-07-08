package org.dromara.northstar.common.model;

import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.indicator.model.Num;

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
	
	public TimeSeriesValue(Num num) {
		this.value = num.value();
		this.timestamp = num.timestamp();
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

	@Override
	public String toString() {
		return "TimeSeriesValue [value=" + value + ", t=" + CommonUtils.millsToLocalDateTime(timestamp) + ", unsettled=" + unsettled + "]";
	}
	
}
