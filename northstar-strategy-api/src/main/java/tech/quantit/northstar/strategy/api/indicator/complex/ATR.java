package tech.quantit.northstar.strategy.api.indicator.complex;

import static tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions.MA;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.google.common.util.concurrent.AtomicDouble;

import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * TR : MAX(MAX((HIGH-LOW),ABS(REF(CLOSE,1)-HIGH)),ABS(REF(CLOSE,1)-LOW));//求最高价减去最低价，一个周期前的收盘价减去最高价的绝对值，一个周期前的收盘价减去最低价的绝对值，这三个值中的最大值
 * ATR : MA(TR,N);//求N个周期内的TR的简单移动平均
 * @author KevinHuangwl
 *
 */
public class ATR {
	
	/**
	 * 获取ATR计算函数
	 * 仅适用于日线周期
	 * @param n		统计N个K线周期
	 * @return
	 */
	public static Function<BarField, TimeSeriesValue> ofBar(int n){
		final AtomicDouble lastClose = new AtomicDouble();
		final TimeSeriesUnaryOperator ma = MA(n);
		return bar -> {
			double range = bar.getHighPrice() - bar.getLowPrice();
			double maxVal = lastClose.get() == 0 
					? range
					: Math.max(range, Math.max(Math.abs(lastClose.get() - bar.getHighPrice()), Math.abs(lastClose.get() - bar.getLowPrice())));
			lastClose.set(bar.getClosePrice());
			return ma.apply(new TimeSeriesValue(maxVal, bar.getActionTimestamp()));
		};
	}
	
	/**
	 * 获取广义ATR计算函数
	 * 仅适用于分钟线周期
	 * @param n		统计N天的K线周期
	 * @return
	 */
	public static Function<BarField, TimeSeriesValue> ofDay(int n){
		final double[] ranges = new double[n];
		final AtomicInteger cursor = new AtomicInteger();
		final AtomicDouble highest = new AtomicDouble(Double.MIN_VALUE);
		final AtomicDouble lowest = new AtomicDouble(Double.MAX_VALUE);
		final AtomicDouble closeYd = new AtomicDouble();
		final AtomicDouble lastClose = new AtomicDouble();
		final String[] date = {""};
		return bar -> {
			if(!StringUtils.equals(bar.getTradingDay(), date[0])) {
				double preClose = closeYd.get();
				double trueRange = Math.max(highest.get() - lowest.get(), Math.max(Math.abs(preClose - highest.get()), Math.abs(preClose - lowest.get())));
				date[0] = bar.getTradingDay();
				cursor.set(cursor.incrementAndGet() % n);
				ranges[cursor.get()] = trueRange;
				closeYd.set(lastClose.get());
				highest.set(bar.getOpenPrice());
				lowest.set(bar.getOpenPrice());
			}
			highest.set(Math.max(highest.get(), bar.getHighPrice()));
			lowest.set(Math.min(lowest.get(), bar.getLowPrice()));
			lastClose.set(bar.getClosePrice());
			return new TimeSeriesValue(ranges[cursor.get()], bar.getActionTimestamp());
		};
	}
}
