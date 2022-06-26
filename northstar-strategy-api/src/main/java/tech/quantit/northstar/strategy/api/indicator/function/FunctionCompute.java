package tech.quantit.northstar.strategy.api.indicator.function;

import java.util.function.UnaryOperator;

import tech.quantit.northstar.common.model.TimeSeriesValue;

public interface FunctionCompute {

	static UnaryOperator<TimeSeriesValue> add(UnaryOperator<TimeSeriesValue> fn1, UnaryOperator<TimeSeriesValue> fn2){
		return tv -> {
			TimeSeriesValue v = fn1.apply(tv);
			TimeSeriesValue v0 = fn2.apply(tv);
			v.setValue(v.getValue() + v0.getValue());
			return v;
		};
	}
	
	static UnaryOperator<TimeSeriesValue> minus(UnaryOperator<TimeSeriesValue> fn1, UnaryOperator<TimeSeriesValue> fn2){
		return tv -> {
			TimeSeriesValue v = fn1.apply(tv);
			TimeSeriesValue v0 = fn2.apply(tv);
			v.setValue(v.getValue() - v0.getValue());
			return v;
		};
	} 
}
