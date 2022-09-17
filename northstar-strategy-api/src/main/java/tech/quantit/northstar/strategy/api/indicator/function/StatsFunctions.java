package tech.quantit.northstar.strategy.api.indicator.function;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.stat.StatUtils;
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
	 * N个周期内的标准差函数
	 * @param n		统计范围
	 * @return		返回计算函数
	 */
	static TimeSeriesUnaryOperator STD(int n){
		final double[] values = new double[n];
		final AtomicInteger cursor = new AtomicInteger();
		final StandardDeviation std = new StandardDeviation();
		return tv ->{
			if(tv.isUnsettled()) {
				double[] valuesCopy = new double[n];
				System.arraycopy(values, 0, valuesCopy, 0, n);
				valuesCopy[cursor.get()] = tv.getValue();
				double stdVal = std.evaluate(values);
				return new TimeSeriesValue(stdVal, tv.getTimestamp(), tv.isUnsettled());
			}
			
			values[cursor.get()] = tv.getValue();
			cursor.set(cursor.incrementAndGet() % n);
			double stdVal = std.evaluate(values);
			return new TimeSeriesValue(stdVal, tv.getTimestamp());
		};
	}
	
	/**
	 * N个周期内的最低价计算函数
	 * @param n		统计范围
	 * @return		返回计算函数
	 */
	static TimeSeriesUnaryOperator LLV(int n) {
		final double[] values = new double[n];
		final AtomicInteger cursor = new AtomicInteger();
		return tv -> {
			if(tv.isUnsettled()) {
				double[] valuesCopy = new double[n];
				System.arraycopy(values, 0, valuesCopy, 0, n);
				valuesCopy[cursor.get()] = tv.getValue();
				return new TimeSeriesValue(StatUtils.min(valuesCopy), tv.getTimestamp(), tv.isUnsettled());
			}
			
			values[cursor.get()] = tv.getValue();
			cursor.set(cursor.incrementAndGet() % n);
			return new TimeSeriesValue(StatUtils.min(values), tv.getTimestamp());
		};
	}
	
	/**
	 * N个周期内的最高价计算函数
	 * @param n		统计范围
	 * @return		返回计算函数
	 */
	static TimeSeriesUnaryOperator HHV(int n) {
		final double[] values = new double[n];
		final AtomicInteger cursor = new AtomicInteger();
		return tv -> {
			if(tv.isUnsettled()) {
				double[] valuesCopy = new double[n];
				System.arraycopy(values, 0, valuesCopy, 0, n);
				valuesCopy[cursor.get()] = tv.getValue();
				return new TimeSeriesValue(StatUtils.max(valuesCopy), tv.getTimestamp(), tv.isUnsettled());
			}
			
			values[cursor.get()] = tv.getValue();
			cursor.set(cursor.incrementAndGet() % n);
			return new TimeSeriesValue(StatUtils.max(values), tv.getTimestamp());
		};
	}

}
