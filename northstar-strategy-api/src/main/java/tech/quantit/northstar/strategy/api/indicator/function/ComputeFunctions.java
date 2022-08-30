package tech.quantit.northstar.strategy.api.indicator.function;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.google.common.util.concurrent.AtomicDouble;

import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import xyz.redtorch.pb.CoreField.BarField;

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
			v.setValue(v.getValue() + v0.getValue());
			return v;
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
			v.setValue(v.getValue() - v0.getValue());
			return v;
		};
	}

	/**
	 * 两线距离
	 * @param line1
	 * @param line2
	 * @return
	 */
	static Function<BarField, TimeSeriesValue> diff(Function<BarField, TimeSeriesValue> line1Fn, Function<BarField, TimeSeriesValue> line2Fn) {
		return bar -> {
			TimeSeriesValue v = line1Fn.apply(bar);
			TimeSeriesValue v0 = line2Fn.apply(bar);
			double val = v.getValue() - v0.getValue();
			return new TimeSeriesValue(val, bar.getActionTimestamp());
		};
	}
	
	/**
	 * 数值透视
	 * @param valueHolder	
	 * @return
	 */
	static TimeSeriesUnaryOperator display(AtomicDouble valueHolder) {
		return tv -> {
			return new TimeSeriesValue(valueHolder.get(), tv.getTimestamp());
		};
	}
}
