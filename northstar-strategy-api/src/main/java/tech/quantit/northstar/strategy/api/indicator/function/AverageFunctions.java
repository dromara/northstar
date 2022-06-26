package tech.quantit.northstar.strategy.api.indicator.function;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AtomicDouble;

import tech.quantit.northstar.common.model.TimeSeriesValue;

/**
 * 均线函数
 * @author KevinHuangwl
 *
 */
public interface AverageFunctions {

	/**
	 * 指数加权平均EMA
	 * @param size
	 * @return
	 */
	static TimeSeriesUnaryOperator EMA(int size) {
		final AtomicDouble ema = new AtomicDouble();
		final AtomicBoolean hasInitVal = new AtomicBoolean();
		final double factor = 2D / (size + 1);
		return tv -> {
			double val = tv.getValue();
			long timestamp = tv.getTimestamp();
			if(hasInitVal.get()) {			
				ema.set(factor * val + (1 - factor) * ema.get());
			} else {
				ema.set(val);
				hasInitVal.set(true);
			}
			return new TimeSeriesValue(ema.get(), timestamp);
		};
	}
	
	/**
	 * 简单移动平均MA
	 * @param size
	 * @return
	 */
	static TimeSeriesUnaryOperator MA(int size) {
		final double[] values = new double[size];
		final AtomicInteger cursor = new AtomicInteger();
		final AtomicDouble sumOfValues = new AtomicDouble();
		return tv -> {
			long timestamp = tv.getTimestamp();
			double val = tv.getValue();
			double oldVal = values[cursor.get()];
			values[cursor.get()] = val;
			cursor.set(cursor.incrementAndGet() % size);
			sumOfValues.addAndGet(val - oldVal);
			val = sumOfValues.get() / size;
			return new TimeSeriesValue(val, timestamp);
		};
	}
}
