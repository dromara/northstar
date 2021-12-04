package tech.quantit.northstar.strategy.api.model;

import java.util.List;

import lombok.Getter;

@Getter
public class TimeSeriesData {

	private double[] valData;
	
	private int[] timestampOffsetData;
	
	private long startTimestamp;
	
	public TimeSeriesData(List<TimeSeriesValue> data) {
		if(data == null || data.isEmpty())
			throw new IllegalArgumentException("数据不能为空");
		if(data.size() > 10000) 
			throw new UnsupportedOperationException("数据量过大，超过可以支持的数量：10000");
		startTimestamp = data.get(0).getTimestamp();
		valData = data.stream().mapToDouble(TimeSeriesValue::getValue).toArray();
		timestampOffsetData = data.stream().mapToInt(val -> (int)(val.getTimestamp() - startTimestamp)).toArray();
	}
}
