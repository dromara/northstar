package tech.quantit.northstar.strategy.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import tech.quantit.northstar.common.model.BarWrapper;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;

/**
 * 指标工厂
 * @author KevinHuangwl
 *
 */
public class IndicatorFactory {

	private Map<String, Indicator> indicatorMap = new LinkedHashMap<>();
	
	private IModuleContext ctx;
	
	public IndicatorFactory(IModuleContext ctx) {
		this.ctx = ctx;
	}
	
	public Indicator newIndicator(Indicator.Configuration config, ValueType valTypeOfBar, TimeSeriesUnaryOperator valueUpdateHandler) {
		if(indicatorMap.containsKey(config.getIndicatorName())) {
			throw new IllegalArgumentException(String.format("模组 [%s] 创建指标异常： [%s] 指标已存在，不能重名", ctx.getModuleName(), config.getIndicatorName()));
		}
		Indicator indicator = new Indicator(config, valTypeOfBar, valueUpdateHandler);
		indicatorMap.put(config.getIndicatorName(), indicator);
		return indicator;
	}
	
	public Indicator newIndicator(Indicator.Configuration config, Function<BarWrapper, TimeSeriesValue> valueUpdateHandler) {
		if(indicatorMap.containsKey(config.getIndicatorName())) {
			throw new IllegalArgumentException(String.format("模组 [%s] 创建指标异常： [%s] 指标已存在，不能重名", ctx.getModuleName(), config.getIndicatorName()));
		}
		Indicator indicator = new Indicator(config, valueUpdateHandler);
		indicatorMap.put(config.getIndicatorName(), indicator);
		return indicator;
	}
	
	public Map<String, Indicator> getIndicatorMap(){
		return indicatorMap;
	}
}
