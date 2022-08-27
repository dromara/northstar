package tech.quantit.northstar.strategy.api.indicator.complex;

import static tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions.WMA;
import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.HHV;
import static tech.quantit.northstar.strategy.api.indicator.function.StatsFunctions.LLV;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.math3.stat.StatUtils;

import com.google.common.util.concurrent.AtomicDouble;

import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 波浪指标
 * @author KevinHuangwl
 *
 */
public class WAVE {

	private LWR lwr;
	
	private int m;
	
	private int n;
	
	private static final TimeSeriesValue TV_PLACEHOLDER = new TimeSeriesValue(0, 0);
	
	public WAVE(int n, int m) {
		this.n = n;
		this.m = m;
		lwr = LWR.of(n, m, m);
	}
	
	public static WAVE of(int n, int m) {
		return new WAVE(n, m); 
	}
	
	/**
	 * @return	波浪计算函数
	 */
	public Function<BarField, TimeSeriesValue> wave(){
		final Function<BarField, TimeSeriesValue> fast = lwr.fast();
		final Function<BarField, TimeSeriesValue> slow = lwr.slow();
		int ref = this.m * 2;
		final TimeSeriesUnaryOperator barllv = LLV(ref);
		final TimeSeriesUnaryOperator barhhv = HHV(ref);
		final TimeSeriesUnaryOperator fastllv = LLV(ref);
		final TimeSeriesUnaryOperator fasthhv = HHV(ref);
		final AtomicDouble lastFast = new AtomicDouble();
		final AtomicDouble lastSlow = new AtomicDouble();
		return bar -> {
			TimeSeriesValue barllvV = barllv.apply(new TimeSeriesValue(bar.getLowPrice(), bar.getActionTimestamp()));
			TimeSeriesValue barhhvV = barhhv.apply(new TimeSeriesValue(bar.getHighPrice(), bar.getActionTimestamp()));
			TimeSeriesValue fastV = fast.apply(bar);
			TimeSeriesValue slowV = slow.apply(bar);
			TimeSeriesValue fastllvV = fastllv.apply(new TimeSeriesValue(fastV.getValue(), bar.getActionTimestamp()));
			TimeSeriesValue fasthhvV = fasthhv.apply(new TimeSeriesValue(fastV.getValue(), bar.getActionTimestamp()));
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
			lastFast.set(fastV.getValue());
			lastSlow.set(slowV.getValue());
			return result;
		};
	}
	
	/**
	 * 
	 * @param num	统计数量
	 * @return		浪顶均值		
	 */
	public Function<BarField, TimeSeriesValue> peak(int num){
		final Function<BarField, TimeSeriesValue> wma = WMA(n);
		final Function<BarField, TimeSeriesValue> wave = wave();
		final double[] valArr = new double[num];
		final AtomicInteger cursor = new AtomicInteger();
		final AtomicBoolean flag = new AtomicBoolean();
		return bar -> {
			TimeSeriesValue wmaVal = wma.apply(bar);
			TimeSeriesValue waveVal = wave.apply(bar);
			if(StatUtils.mean(valArr) == 0) {	// 初始化数组
				Arrays.fill(valArr, bar.getHighPrice());
			}
			if(waveVal != TV_PLACEHOLDER) {
				boolean oldFlag = flag.get();
				flag.set(waveVal.getValue() > wmaVal.getValue());		// 判断是否为浪顶
				if(flag.get()) {
					// 如果上一个也是浪顶，替换上一个的值；否则下标加1，插入新的浪顶值
					if(oldFlag) {	
						valArr[cursor.get()] = Math.max(waveVal.getValue(), valArr[cursor.get()]);
					} else {
						cursor.set(cursor.incrementAndGet() % num);
						valArr[cursor.get()] = waveVal.getValue();
					}
				}
			}
			return new TimeSeriesValue(StatUtils.max(valArr), bar.getActionTimestamp());
		};
	}
	
	/**
	 * 
	 * @param num	统计数量
	 * @return		浪底均值		
	 */
	public Function<BarField, TimeSeriesValue> trough(int num){
		final Function<BarField, TimeSeriesValue> wma = WMA(n);
		final Function<BarField, TimeSeriesValue> wave = wave();
		final double[] valArr = new double[num];
		final AtomicInteger cursor = new AtomicInteger();
		final AtomicBoolean flag = new AtomicBoolean();
		return bar -> {
			TimeSeriesValue wmaVal = wma.apply(bar);
			TimeSeriesValue waveVal = wave.apply(bar);
			if(StatUtils.mean(valArr) == 0) {	// 初始化数组
				Arrays.fill(valArr, bar.getLowPrice());
			}
			if(waveVal != TV_PLACEHOLDER) {
				boolean oldFlag = flag.get();
				flag.set(waveVal.getValue() < wmaVal.getValue());		// 判断是否为浪底
				if(flag.get()) {
					// 如果上一个也是浪顶，替换上一个的值；否则下标加1，插入新的浪顶值
					if(oldFlag) {	
						valArr[cursor.get()] = Math.min(waveVal.getValue(), valArr[cursor.get()]);
					} else {
						cursor.set(cursor.incrementAndGet() % num);
						valArr[cursor.get()] = waveVal.getValue();
					}
				}
			}
			return new TimeSeriesValue(StatUtils.min(valArr), bar.getActionTimestamp());
		};
	}
	
}
