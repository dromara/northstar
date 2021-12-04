package tech.xuanwu.northstar.strategy.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeSeriesValue {

	private double value;
	
	private long timestamp;
}
