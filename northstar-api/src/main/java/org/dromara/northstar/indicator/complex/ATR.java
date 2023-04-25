package org.dromara.northstar.indicator.complex;

import static org.dromara.northstar.indicator.function.AverageFunctions.MA;

import java.util.function.Function;

import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.strategy.TimeSeriesUnaryOperator;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * TR : MAX(MAX((HIGH-LOW),ABS(REF(CLOSE,1)-HIGH)),ABS(REF(CLOSE,1)-LOW));//求最高价减去最低价，一个周期前的收盘价减去最高价的绝对值，一个周期前的收盘价减去最低价的绝对值，这三个值中的最大值
 * ATR : MA(TR,N);//求N个周期内的TR的简单移动平均
 * @author KevinHuangwl
 *
 */
@Deprecated
public class ATR {
	
	/**
	 * 获取ATR计算函数
	 * 仅适用于日线周期
	 * @param n		统计N个K线周期
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> of(int n){
		final AtomicDouble lastClose = new AtomicDouble();
		final TimeSeriesUnaryOperator ma = MA(n);
		return bar -> {
			double range = bar.getBar().getHighPrice() - bar.getBar().getLowPrice();
			double maxVal = lastClose.get() == 0 
					? range
					: Math.max(range, Math.max(Math.abs(lastClose.get() - bar.getBar().getHighPrice()), Math.abs(lastClose.get() - bar.getBar().getLowPrice())));
			if(!bar.isUnsettled()) {				
				lastClose.set(bar.getBar().getClosePrice());
			}
			return ma.apply(new TimeSeriesValue(maxVal, bar.getBar().getActionTimestamp(), bar.isUnsettled()));
		};
	}
	
}
