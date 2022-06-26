package tech.quantit.northstar.strategy.api.indicator.function;

import java.util.Objects;

import tech.quantit.northstar.common.model.TimeSeriesValue;

public interface FunctionCompute {

	static TimeSeriesUnaryOperator add(TimeSeriesUnaryOperator fn1, TimeSeriesUnaryOperator fn2){
		Objects.requireNonNull(fn1);
		Objects.requireNonNull(fn2);
		return tv -> {
			TimeSeriesValue v = fn1.apply(tv);
			TimeSeriesValue v0 = fn2.apply(tv);
			v.setValue(v.getValue() + v0.getValue());
			return v;
		};
	}
	
	static TimeSeriesUnaryOperator minus(TimeSeriesUnaryOperator fn1, TimeSeriesUnaryOperator fn2){
		Objects.requireNonNull(fn1);
		Objects.requireNonNull(fn2);
		return tv -> {
			TimeSeriesValue v = fn1.apply(tv);
			TimeSeriesValue v0 = fn2.apply(tv);
			v.setValue(v.getValue() - v0.getValue());
			return v;
		};
	} 
	
}
