package tech.quantit.northstar.strategy.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;

/**
 * 指标工厂
 * @author KevinHuangwl
 *
 */
public class IndicatorFactory {

	private Map<String, Indicator> indicatorMap = new LinkedHashMap<>();
	
	public Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, int indicatorLength, ValueType valTypeOfBar,
			DoubleUnaryOperator valueUpdateHandler) {
		if(indicatorMap.containsKey(indicatorName)) {
			throw new IllegalArgumentException(String.format("[%s] 指标已存在，不能重名", indicatorName));
		}
		Indicator indicator = new Indicator(bindedUnifiedSymbol, indicatorLength, valTypeOfBar, valueUpdateHandler);
		indicatorMap.put(indicatorName, indicator);
		return indicator;
	}
	
	public Map<String, Indicator> getIndicatorMap(){
		return indicatorMap;
	}
}
