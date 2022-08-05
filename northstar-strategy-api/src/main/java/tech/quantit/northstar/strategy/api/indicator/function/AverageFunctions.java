package tech.quantit.northstar.strategy.api.indicator.function;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.google.common.util.concurrent.AtomicDouble;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 均线函数
 * @author KevinHuangwl
 *
 */
public interface AverageFunctions {

	/**
	 * 当日成交量加权均价（当日结算价）
	 * 注意：该算法与交易所的结算价存在一定误差，主要因为该算法是按K线计算，K线周期越小，误差越小
	 * @param
	 * @param
	 * @return
	 */
	static Function<BarField, TimeSeriesValue> SETTLE(){
		final AtomicDouble weightPrice = new AtomicDouble();
		final AtomicInteger countOfBarsToday = new AtomicInteger();
		final AtomicLong sumVol = new AtomicLong();
		final String[] tradeDay = {""};
		return bar -> {
			if(!StringUtils.equals(tradeDay[0], bar.getTradingDay())) {
				tradeDay[0] = bar.getTradingDay();
				sumVol.set(0);
				weightPrice.set(0);
				countOfBarsToday.set(0);
			}
			countOfBarsToday.incrementAndGet();
			sumVol.addAndGet(bar.getVolumeDelta());
			double wp = (bar.getHighPrice() + bar.getLowPrice() + bar.getClosePrice() * 2) / 4;
			double factor = 1.0 * bar.getVolumeDelta() / sumVol.get();
			double value = factor * wp + (1 - factor) * weightPrice.get();
			weightPrice.set(value);
			return new TimeSeriesValue(value, bar.getActionTimestamp());
		};
	}

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

	/**
	 * 函数：STD
	 * 说明:估算标准差
	 * 用法:STD(size)为收盘价的size日估算标准差
	 * @param size
	 * @return
	 */
	static TimeSeriesUnaryOperator STD(int size){
		final double[] values = new double[size];
		final AtomicInteger cursor = new AtomicInteger();
		return tv ->{
			long timestamp = tv.getTimestamp();
			values[cursor.get()] = tv.getValue();
			cursor.set(cursor.incrementAndGet() % size);
			double variance = new StandardDeviation().evaluate(values);
			return new TimeSeriesValue(variance, timestamp);
		};
	}
}
