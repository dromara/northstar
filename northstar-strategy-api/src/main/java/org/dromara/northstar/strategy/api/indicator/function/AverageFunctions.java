package org.dromara.northstar.strategy.api.indicator.function;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.LongStream;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * 均线函数
 * 函数名称为了与业界实践保持一致，并没有僵硬地采用驼峰命名规范，而是遵循业界常用命名
 * @author KevinHuangwl
 *
 */
public interface AverageFunctions {
	
	/**
	 * 当日成交量加权均价（当日结算价）计算函数
	 * 注意：该算法与交易所的结算价存在一定误差，主要因为该算法是按K线计算，K线周期越小，误差越小
	 * @return		返回计算函数
	 */
	static Function<BarWrapper, TimeSeriesValue> SETTLE(){
		final AtomicDouble weightPrice = new AtomicDouble();
		final AtomicLong sumVol = new AtomicLong();
		final String[] tradeDay = {""};
		return bar -> {
			if(bar.isUnsettled()) {
				long sumVolTmp = 0;
				double wPrice = 0;
				if(StringUtils.equals(tradeDay[0], bar.getBar().getTradingDay())) {
					sumVolTmp = sumVol.get();
					wPrice = weightPrice.get();
				}
				
				long sumVolTemp = sumVolTmp + bar.getBar().getVolume(); 
				double wp = (bar.getBar().getHighPrice() + bar.getBar().getLowPrice() + bar.getBar().getClosePrice() * 2) / 4;
				double factor = 1.0 * bar.getBar().getVolume() / sumVolTemp;
				double value = factor * wp + (1 - factor) * wPrice;
				return new TimeSeriesValue(value, bar.getBar().getActionTimestamp(), bar.isUnsettled());
			}
			
			if(!StringUtils.equals(tradeDay[0], bar.getBar().getTradingDay())) {
				tradeDay[0] = bar.getBar().getTradingDay();
				sumVol.set(0);
				weightPrice.set(0);
			}
			sumVol.addAndGet(bar.getBar().getVolume());
			double wp = (bar.getBar().getHighPrice() + bar.getBar().getLowPrice() + bar.getBar().getClosePrice() * 2) / 4;
			double factor = 1.0 * bar.getBar().getVolume() / sumVol.get();
			double value = factor * wp + (1 - factor) * weightPrice.get();
			weightPrice.set(value);
			return new TimeSeriesValue(value, bar.getBar().getActionTimestamp());
		};
	}

	/**
	 * N周期内的成交量加权均价计算函数
	 * @param n		统计范围
	 * @return		返回计算函数
	 */
	static Function<BarWrapper, TimeSeriesValue> WMA(int n){
		final long[] volArr = new long[n];
		final double[] priceArr = new double[n];
		final AtomicInteger index = new AtomicInteger(0);
		return bar -> {
			int i = index.get();
			if(bar.isUnsettled()) {
				long total = LongStream.of(volArr).sum() - volArr[i] + bar.getBar().getVolume();
				double wPrice = (bar.getBar().getClosePrice() * 2 + bar.getBar().getHighPrice() + bar.getBar().getLowPrice()) / 4;
				if(total == 0) {
					return new TimeSeriesValue(0, 0);
				}
				double weightedSum = 0;
				for(int j=0; j<n; j++) {
					if(j == i) {
						weightedSum += wPrice * bar.getBar().getVolume() / total;
					} else {						
						weightedSum += priceArr[j] * volArr[j] / total;
					}
				}
				return new TimeSeriesValue(weightedSum, bar.getBar().getActionTimestamp(), bar.isUnsettled());
			}
			
			volArr[i] = bar.getBar().getVolume();
			priceArr[i] = (bar.getBar().getClosePrice() * 2 + bar.getBar().getHighPrice() + bar.getBar().getLowPrice()) / 4;	// 利用K线重心为计算依据
			index.set(++i % n);
			long total = LongStream.of(volArr).sum();
			if(total == 0) {
				return new TimeSeriesValue(0, 0);
			}
			double weightedSum = 0;
			for(int j=0; j<n; j++) {
				weightedSum += priceArr[j] * volArr[j] / total;
			}
			return new TimeSeriesValue(weightedSum, bar.getBar().getActionTimestamp());
		};
	}

	/**
	 * 指数加权平均EMA计算函数
	 * @param n		统计范围
	 * @return		返回计算函数
	 */
	static TimeSeriesUnaryOperator EMA(int n) {
		final AtomicDouble ema = new AtomicDouble();
		final AtomicBoolean hasInitVal = new AtomicBoolean();
		final double factor = 2D / (n + 1);
		return tv -> {
			if(tv.isUnsettled()) {
				double val = hasInitVal.get() ? factor * tv.getValue() + (1 - factor) * ema.get() : tv.getValue();
				return new TimeSeriesValue(val, tv.getTimestamp(), tv.isUnsettled());
			}
			
			double val = tv.getValue();
			if(hasInitVal.get()) {
				ema.set(factor * val + (1 - factor) * ema.get());
			} else {
				ema.set(val);
				hasInitVal.set(true);
			}
			return new TimeSeriesValue(ema.get(), tv.getTimestamp());
		};
	}

	/**
	 * 扩展指数加权移动平均SMA计算函数
	 * @param n		统计范围
	 * @param m		权重
	 * @return		返回计算函数
	 */
	static TimeSeriesUnaryOperator SMA(int n, int m) {
		final AtomicDouble sma = new AtomicDouble();
		final AtomicBoolean hasInitVal = new AtomicBoolean();
		final double factor = (double) m / n;
		return tv -> {
			if(tv.isUnsettled()) {
				double val = hasInitVal.get() ? factor * tv.getValue() + (1 - factor) * sma.get() : tv.getValue();
				return new TimeSeriesValue(val, tv.getTimestamp(), tv.isUnsettled());
			}
			
			double val = tv.getValue();
			if(hasInitVal.get()) {
				sma.set(factor * val + (1 - factor) * sma.get());
			} else {
				sma.set(val);
				hasInitVal.set(true);
			}
			return new TimeSeriesValue(sma.get(), tv.getTimestamp());
		};
	}

	/**
	 * 简单移动平均MA计算函数
	 * @param n		统计范围
	 * @return		返回计算函数
	 */
	static TimeSeriesUnaryOperator MA(int n) {
		final double[] values = new double[n];
		final AtomicInteger cursor = new AtomicInteger();
		final AtomicDouble sumOfValues = new AtomicDouble();
		return tv -> {
			int i = cursor.get();
			if(tv.isUnsettled()) {
				double val = (sumOfValues.get() - values[i] + tv.getValue()) / n ;
				return new TimeSeriesValue(val, tv.getTimestamp(), tv.isUnsettled());
			}
			
			double val = tv.getValue();
			double oldVal = values[i];
			values[i] = val;
			cursor.set(cursor.incrementAndGet() % n);
			sumOfValues.addAndGet(val - oldVal);
			val = sumOfValues.get() / n;
			return new TimeSeriesValue(val, tv.getTimestamp());
		};
	}

}
