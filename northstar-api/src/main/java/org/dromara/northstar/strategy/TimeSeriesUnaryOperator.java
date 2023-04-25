package org.dromara.northstar.strategy;

import java.util.Objects;
import java.util.function.UnaryOperator;

import org.dromara.northstar.common.model.TimeSeriesValue;

/**
 * 
 * @author KevinHuangwl
 *
 */
@Deprecated
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
