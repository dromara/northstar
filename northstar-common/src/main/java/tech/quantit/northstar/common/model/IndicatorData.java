package tech.quantit.northstar.common.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import tech.quantit.northstar.common.constant.IndicatorType;

@Data
@Builder
public class IndicatorData {
	
	private String unifiedSymbol;
	
	private List<TimeSeriesValue> values;
	
	private IndicatorType type;
}
