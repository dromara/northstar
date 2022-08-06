package tech.quantit.northstar.strategy.api.indicator.function;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;

/**
 * 统计函数
 * 函数名称为了与业界实践保持一致，并没有僵硬地采用驼峰命名规范，而是遵循业界常用命名
 * @author KevinHuangwl
 *
 */
public interface StatsFunctions {
	
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
