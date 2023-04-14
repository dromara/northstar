package org.dromara.northstar.strategy.api.indicator.complex;

import static org.dromara.northstar.strategy.api.indicator.function.AverageFunctions.SMA;

import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * LC := REF(CLOSE,1);//前一周期收盘价
 * RSI1:SMA(MAX(CLOSE-LC,0),N1,1)/SMA(ABS(CLOSE-LC),N1,1)*100;//当根K线收盘价与前一周期收盘价做差，在该差值与0之间取最大值，做N1周期移动平均。收盘价与前一周期收盘价做差值，取该差值的N1周期移动平均值，两平均值之间做比值。
 * RSI2:SMA(MAX(CLOSE-LC,0),N2,1)/SMA(ABS(CLOSE-LC),N2,1)*100;//当根K线收盘价与前一周期收盘价做差，在该差值与0之间取最大值，做N2周期移动平均。收盘价与前一周期收盘价做差值，取该差值的N2周期移动平均值，两平均值之间做比值。
 * @author KevinHuangwl
 *
 */
public class RSI {

	/**
	 * 获取RSI计算函数
	 * @param n
	 * @return
	 */
	public static TimeSeriesUnaryOperator line(int n) {
		final TimeSeriesUnaryOperator sma1 = SMA(n, 1);
		final TimeSeriesUnaryOperator sma2 = SMA(n, 1);
		final AtomicDouble lastVal = new AtomicDouble();
		return tv -> {
			double val = lastVal.get() == 0 ? 0 : tv.getValue() - lastVal.get();
			if(!tv.isUnsettled()) {				
				lastVal.set(tv.getValue());
			}
			TimeSeriesValue v1 = sma1.apply(new TimeSeriesValue(Math.max(val, 0), tv.getTimestamp(), tv.isUnsettled()));
			TimeSeriesValue v2 = sma2.apply(new TimeSeriesValue(Math.abs(val), tv.getTimestamp(), tv.isUnsettled()));
			return new TimeSeriesValue(v1.getValue() / v2.getValue() * 100, tv.getTimestamp(), tv.isUnsettled());
		};
	}
	
}
