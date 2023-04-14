package org.dromara.northstar.strategy.api.indicator.complex;

import java.util.function.Function;

import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import org.dromara.northstar.strategy.api.indicator.function.AverageFunctions;

/**
 * 瀑布线
 * @author KevinHuangwl
 *
 */
public class PBX {

	/**
	 * 采用成交量加权算法
	 * @param m
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> wline(int m){
		final Function<BarWrapper, TimeSeriesValue> wma = AverageFunctions.WMA(m);
		final Function<BarWrapper, TimeSeriesValue> wma2 = AverageFunctions.WMA(m*2);
		final Function<BarWrapper, TimeSeriesValue> wma4 = AverageFunctions.WMA(m*4);
		return bar -> {
			TimeSeriesValue v1 = wma.apply(bar);
			TimeSeriesValue v2 = wma2.apply(bar);
			TimeSeriesValue v4 = wma4.apply(bar);
			double avgVal = (v1.getValue() + v2.getValue() + v4.getValue()) / 3;
			return new TimeSeriesValue(avgVal, bar.getBar().getActionTimestamp(), bar.isUnsettled());
		};
	}
	
	/**
	 * 采用传统的均线算法
	 * @param m
	 * @return
	 */
	public static TimeSeriesUnaryOperator line(int m){
		final TimeSeriesUnaryOperator ema = AverageFunctions.EMA(m);
		final TimeSeriesUnaryOperator ma2 = AverageFunctions.MA(m*2);
		final TimeSeriesUnaryOperator ma4 = AverageFunctions.MA(m*4);
		return tv -> {
			TimeSeriesValue v1 = ema.apply(tv);
			TimeSeriesValue v2 = ma2.apply(tv);
			TimeSeriesValue v4 = ma4.apply(tv);
			double avgVal = (v1.getValue() + v2.getValue() + v4.getValue()) / 3;
			return new TimeSeriesValue(avgVal, tv.getTimestamp(), tv.isUnsettled());
		};
	}
}
