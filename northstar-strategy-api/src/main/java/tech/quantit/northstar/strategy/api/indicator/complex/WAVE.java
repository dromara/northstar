package tech.quantit.northstar.strategy.api.indicator.complex;

import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.HHV;
import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.LLV;

import java.util.function.Function;

import com.google.common.util.concurrent.AtomicDouble;

import tech.quantit.northstar.common.model.BarWrapper;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;

/**
 * 波浪指标
 * @author KevinHuangwl
 *
 */
public class WAVE {

	private LWR lwr;
	
	private int m;
	
	private static final TimeSeriesValue TV_PLACEHOLDER = new TimeSeriesValue(0, 0);
	
	public WAVE(int n, int m) {
		this.m = m;
		lwr = LWR.of(n, m, m);
	}
	
	public static WAVE of(int n, int m) {
		return new WAVE(n, m); 
	}
	
	/**
	 * @return	波浪计算函数
	 */
	public Function<BarWrapper, TimeSeriesValue> wave(){
		final Function<BarWrapper, TimeSeriesValue> fast = lwr.fast();
		final Function<BarWrapper, TimeSeriesValue> slow = lwr.slow();
		int ref = this.m * 2;
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
			TimeSeriesValue fastllvV = fastllv.apply(new TimeSeriesValue(fastV.getValue(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			TimeSeriesValue fasthhvV = fasthhv.apply(new TimeSeriesValue(fastV.getValue(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			TimeSeriesValue result = TV_PLACEHOLDER; // 空值
			if(lastFast.get() == 0) 
				lastFast.set(fastV.getValue());
			if(lastSlow.get() == 0) 
				lastSlow.set(slowV.getValue());
			if(lastFast.get() < lastSlow.get() && fastV.getValue() > slowV.getValue() && fastllvV.getValue() < -60) {
				result = barllvV;
			}
			if(lastFast.get() > lastSlow.get() && fastV.getValue() < slowV.getValue() && fasthhvV.getValue() > -40) {
				result = barhhvV;
			}
			if(!bar.isUnsettled()) {				
				lastFast.set(fastV.getValue());
				lastSlow.set(slowV.getValue());
			}
			return result;
		};
	}
	
}
