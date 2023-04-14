package org.dromara.northstar.strategy.api.indicator;

import java.util.Objects;
import java.util.function.UnaryOperator;

import tech.quantit.northstar.common.model.TimeSeriesValue;

/**
 * 
 * @author KevinHuangwl
 *
 */
@FunctionalInterface
public interface TimeSeriesUnaryOperator extends UnaryOperator<TimeSeriesValue> {
	
	TimeSeriesValue apply(TimeSeriesValue t); 
	
	static TimeSeriesUnaryOperator identity() {
		return tv -> tv;
	};

	default TimeSeriesUnaryOperator compose(TimeSeriesUnaryOperator before) {
		Objects.requireNonNull(before);
		return v -> apply(before.apply(v));
	}

	default TimeSeriesUnaryOperator andThen(TimeSeriesUnaryOperator after) {
		Objects.requireNonNull(after);
		return v -> after.apply(apply(v));
	}

}
