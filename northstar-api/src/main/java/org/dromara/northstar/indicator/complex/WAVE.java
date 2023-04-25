package org.dromara.northstar.indicator.complex;

import static org.dromara.northstar.indicator.function.StatsFunctions.HHV;
import static org.dromara.northstar.indicator.function.StatsFunctions.LLV;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.indicator.function.AverageFunctions;
import org.dromara.northstar.strategy.TimeSeriesUnaryOperator;

import com.google.common.collect.Streams;
import com.google.common.util.concurrent.AtomicDouble;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 波浪指标
 * @author KevinHuangwl
 *
 */
@Deprecated
public class WAVE {

	private static final TimeSeriesValue TV_PLACEHOLDER = new TimeSeriesValue(0, 0);
	
	/**
	 * 基于威廉指标定义波浪
	 * @param n
	 * @param m
	 * @return	
	 */
	public static Function<BarWrapper, TimeSeriesValue> wr(int n, int m){
		final LWR lwr = LWR.of(n, m, m);
		final Function<BarWrapper, TimeSeriesValue> fast = lwr.fast();
		final Function<BarWrapper, TimeSeriesValue> slow = lwr.slow();
		int ref = m * 2;
		final TimeSeriesUnaryOperator barllv = LLV(ref);
		final TimeSeriesUnaryOperator barhhv = HHV(ref);
		final TimeSeriesUnaryOperator fastllv = LLV(ref);
		final TimeSeriesUnaryOperator fasthhv = HHV(ref);
		final AtomicDouble lastFast = new AtomicDouble();
		final AtomicDouble lastSlow = new AtomicDouble();
		return bar -> {
			TimeSeriesValue barllvV = barllv.apply(new TimeSeriesValue(bar.getBar().getLowPrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			TimeSeriesValue barhhvV = barhhv.apply(new TimeSeriesValue(bar.getBar().getHighPrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			TimeSeriesValue fastV = fast.apply(bar);
			TimeSeriesValue slowV = slow.apply(bar);
			TimeSeriesValue fastTV = new TimeSeriesValue(fastV.getValue(), bar.getBar().getActionTimestamp(), bar.isUnsettled());
			TimeSeriesValue fastllvV = fastllv.apply(fastTV);
			TimeSeriesValue fasthhvV = fasthhv.apply(fastTV);
			TimeSeriesValue result = TV_PLACEHOLDER; // 空值
			if(lastFast.get() == 0) 
				lastFast.set(fastV.getValue());
			if(lastSlow.get() == 0) 
				lastSlow.set(slowV.getValue());
			if(lastFast.get() < lastSlow.get() && fastV.getValue() > slowV.getValue() && fastllvV.getValue() < -60) 
				result = barllvV;
			if(lastFast.get() > lastSlow.get() && fastV.getValue() < slowV.getValue() && fasthhvV.getValue() > -40) 
				result = barhhvV;
			if(!bar.isUnsettled()) {				
				lastFast.set(fastV.getValue());
				lastSlow.set(slowV.getValue());
			}
			return result;
		};
	}
	
	/**
	 * 基于MACD定义波浪
	 * @param n1
	 * @param n2
	 * @param m
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> macd(int n1, int n2, int m){
		final MACD macd = MACD.of(n1, n2, m);
		final TimeSeriesUnaryOperator diff = macd.diff();
		final TimeSeriesUnaryOperator dea = macd.dea();
		int ref = m * 2;
		final TimeSeriesUnaryOperator barllv = LLV(ref);
		final TimeSeriesUnaryOperator barhhv = HHV(ref);
		final AtomicDouble lastDif = new AtomicDouble();
		final AtomicDouble lastDea = new AtomicDouble();
		return bar -> {
			TimeSeriesValue result = TV_PLACEHOLDER;
			TimeSeriesValue tv = new TimeSeriesValue(bar.getBar().getClosePrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled());
			TimeSeriesValue difVal = diff.apply(tv);
			TimeSeriesValue deaVal = dea.apply(tv);
			TimeSeriesValue llv = barllv.apply(new TimeSeriesValue(bar.getBar().getLowPrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			TimeSeriesValue hhv = barhhv.apply(new TimeSeriesValue(bar.getBar().getHighPrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			if(difVal.getValue() < deaVal.getValue() && lastDif.get() > lastDea.get() && lastDea.get() > 0) 
				result = hhv;
			if(difVal.getValue() > deaVal.getValue() && lastDif.get() < lastDea.get() && lastDea.get() < 0) 
				result = llv;
			if(!bar.isUnsettled()) {
				lastDif.set(difVal.getValue());
				lastDea.set(deaVal.getValue());
			}
			return result;
		};
	}
	
	/**
	 * 基于MA均线定义波浪
	 * @param n
	 * @param m
	 * @param useClosePrice	波浪端点采用收盘价
	 * @return
	 */
	public static Function<BarWrapper, TimeSeriesValue> ma(int n, int m, boolean useClosePrice) {
		final TimeSeriesUnaryOperator maFn = AverageFunctions.MA(n);
		final LinkedList<BarField> cacheBars = new LinkedList<>();
		final LinkedList<Double> maVals = new LinkedList<>();
		final AtomicBoolean currentLong = new AtomicBoolean();
		return bar -> {
			TimeSeriesValue result = TV_PLACEHOLDER;
			TimeSeriesValue avg = maFn.apply(new TimeSeriesValue(bar.getBar().getClosePrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			if(!bar.isUnsettled()) {
				cacheBars.offerFirst(bar.getBar());
				maVals.offerFirst(avg.getValue());
				if(cacheBars.size() > m) {
					List<BarField> firstMBars = cacheBars.subList(0, m).stream().toList();
					List<Double> firstMVals = maVals.subList(0, m).stream().toList();
					int sum = Streams.zip(firstMBars.stream(), firstMVals.stream(), (bf, maVal) -> bf.getClosePrice() > maVal ? 1 : -1)
								.mapToInt(Integer::intValue)
								.sum();
					if(!currentLong.get() && sum == m) {
						currentLong.set(true);
						double latelyLow = useClosePrice 
								? cacheBars.stream().mapToDouble(BarField::getClosePrice).min().orElse(0) 
								: cacheBars.stream().mapToDouble(BarField::getLowPrice).min().orElse(0);
						cacheBars.clear();
						maVals.clear();
						cacheBars.addAll(firstMBars);
						maVals.addAll(firstMVals);
						return new TimeSeriesValue(latelyLow, bar.getBar().getActionTimestamp());
					}
					if(currentLong.get() && sum == -m) {
						currentLong.set(false);
						double latelyHigh = useClosePrice
								? cacheBars.stream().mapToDouble(BarField::getClosePrice).max().orElse(0)
								: cacheBars.stream().mapToDouble(BarField::getHighPrice).max().orElse(0);
						cacheBars.clear();
						maVals.clear();
						cacheBars.addAll(firstMBars);
						maVals.addAll(firstMVals);
						return new TimeSeriesValue(latelyHigh, bar.getBar().getActionTimestamp());
					}
				}
			}
			return result;
		};
	}
}
