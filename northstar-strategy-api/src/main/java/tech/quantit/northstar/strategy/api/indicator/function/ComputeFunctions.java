package tech.quantit.northstar.strategy.api.indicator.function;

import java.util.Objects;
import java.util.function.Function;

import tech.quantit.northstar.common.model.BarWrapper;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;

/**
 * 多函数计算
 * @author KevinHuangwl
 *
 */
public interface ComputeFunctions {

	/**
	 * 函数相加
	 * @param fn1
	 * @param fn2
	 * @return
	 */
	static TimeSeriesUnaryOperator add(TimeSeriesUnaryOperator fn1, TimeSeriesUnaryOperator fn2){
		Objects.requireNonNull(fn1);
		Objects.requireNonNull(fn2);
		return tv -> {
			TimeSeriesValue v = fn1.apply(tv);
			TimeSeriesValue v0 = fn2.apply(tv);
			return new TimeSeriesValue(v.getValue() + v0.getValue(), tv.getTimestamp(), tv.isUnsettled());
		};
	}

	/**
	 * 函数相减
	 * @param fn1
	 * @param fn2
	 * @return
	 */
	static TimeSeriesUnaryOperator minus(TimeSeriesUnaryOperator fn1, TimeSeriesUnaryOperator fn2){
		Objects.requireNonNull(fn1);
		Objects.requireNonNull(fn2);
		return tv -> {
			TimeSeriesValue v = fn1.apply(tv);
			TimeSeriesValue v0 = fn2.apply(tv);
			return new TimeSeriesValue(v.getValue() - v0.getValue(), tv.getTimestamp(), tv.isUnsettled());
		};
	}

	/**
	 * 两线距离
	 * @param line1
	 * @param line2
	 * @return
	 */
	static Function<BarWrapper, TimeSeriesValue> diff(Function<BarWrapper, TimeSeriesValue> line1Fn, Function<BarWrapper, TimeSeriesValue> line2Fn) {
		return bar -> {
			TimeSeriesValue v = line1Fn.apply(bar);
			TimeSeriesValue v0 = line2Fn.apply(bar);
			return new TimeSeriesValue(v.getValue() - v0.getValue(), bar.getBar().getActionTimestamp(), bar.isUnsettled());
		};
	}
}
