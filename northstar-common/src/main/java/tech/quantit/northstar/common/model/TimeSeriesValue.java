package tech.quantit.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeSeriesValue implements Comparable<TimeSeriesValue> {

	private double value;
	
	private long timestamp;

	@Override
	public int compareTo(TimeSeriesValue o) {
		return value < o.value ? -1 : 1;
	}
}
