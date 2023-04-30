package org.dromara.northstar.indicator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.TimeSeriesUnaryOperator;
import org.dromara.northstar.strategy.constant.ValueType;
import org.dromara.northstar.strategy.model.Configuration;

/**
 * 指标工厂
 * @author KevinHuangwl
 *
 */
public class IndicatorFactory {

	private Map<String, Indicator> indicatorMap = new LinkedHashMap<>();
	
	private IModule module;
	
	public IndicatorFactory(IModule module) {
		this.module = module;
	}
	
	public Indicator newIndicator(Configuration config, ValueType valTypeOfBar, TimeSeriesUnaryOperator valueUpdateHandler) {
		if(indicatorMap.containsKey(config.getIndicatorName())) {
			throw new IllegalArgumentException(String.format("模组 [%s] 创建指标异常： [%s] 指标已存在，不能重名", module.getName(), config.getIndicatorName()));
		}
		Indicator indicator = new Indicator(config, valTypeOfBar, valueUpdateHandler);
		indicatorMap.put(config.getIndicatorName(), indicator);
		return indicator;
	}
	
	public Indicator newIndicator(Configuration config, Function<BarWrapper, TimeSeriesValue> valueUpdateHandler) {
		if(indicatorMap.containsKey(config.getIndicatorName())) {
			throw new IllegalArgumentException(String.format("模组 [%s] 创建指标异常： [%s] 指标已存在，不能重名", module.getName(), config.getIndicatorName()));
		}
		Indicator indicator = new Indicator(config, valueUpdateHandler);
		indicatorMap.put(config.getIndicatorName(), indicator);
		return indicator;
	}
	
	public Map<String, Indicator> getIndicatorMap(){
		return indicatorMap;
	}
}
