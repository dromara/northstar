package tech.quantit.northstar.strategy.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 指标工厂
 * @author KevinHuangwl
 *
 */
public class IndicatorFactory {

	private Map<String, Indicator> indicatorMap = new LinkedHashMap<>();
	
	public Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, int indicatorLength, ValueType valTypeOfBar,
			TimeSeriesUnaryOperator valueUpdateHandler) {
		if(indicatorMap.containsKey(indicatorName)) {
			throw new IllegalArgumentException(String.format("[%s] 指标已存在，不能重名", indicatorName));
		}
		Indicator indicator = new Indicator(bindedUnifiedSymbol, indicatorLength, valTypeOfBar, valueUpdateHandler);
		indicatorMap.put(indicatorName, indicator);
		return indicator;
	}
	
	public Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, int indicatorLength, 
			Function<BarField, TimeSeriesValue> valueUpdateHandler) {
		if(indicatorMap.containsKey(indicatorName)) {
			throw new IllegalArgumentException(String.format("[%s] 指标已存在，不能重名", indicatorName));
		}
		Indicator indicator = new Indicator(bindedUnifiedSymbol, indicatorLength, valueUpdateHandler);
		indicatorMap.put(indicatorName, indicator);
		return indicator;
	}
	
	public Map<String, Indicator> getIndicatorMap(){
		return indicatorMap;
	}
}
