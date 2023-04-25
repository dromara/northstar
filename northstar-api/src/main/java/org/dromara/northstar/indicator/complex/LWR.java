package org.dromara.northstar.indicator.complex;

import static org.dromara.northstar.indicator.function.AverageFunctions.SMA;
import static org.dromara.northstar.indicator.function.StatsFunctions.HHV;
import static org.dromara.northstar.indicator.function.StatsFunctions.LLV;

import java.util.function.Function;

import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.strategy.TimeSeriesUnaryOperator;

/**
 * 威廉指标
 * RSV:= (CLOSE-HHV(HIGH,N))/(HHV(HIGH,N)-LLV(LOW,N))*100;//收盘价与N周期最高值做差，N周期最高值与N周期最低值做差，两差值间做比值。
 * FAST:SMA(RSV,M1,1);//RSV的移动平均
 * SLOW:SMA(FAST,M2,1);//LWR1的移动平均
 * @author KevinHuangwl
 *
 */
@Deprecated
public class LWR {
	
	private int n;
	private int m1;
	private int m2;
	
	/**
	 * 创建威廉指标生成器
	 * @param n		
	 * @param m1
	 * @param m2
	 */
	public LWR(int n, int m1, int m2) {
		this.n = n;
		this.m1 = m1;
		this.m2 = m2;
	}
	
	/**
	 * 创建威廉指标生成器
	 * @param n		
	 * @param m1
	 * @param m2
	 */
	public static LWR of(int n, int m1, int m2) {
		return new LWR(n, m1, m2);
	}
	
	/**
	 * 威廉快线计算函数
	 * RSV:= (CLOSE-HHV(HIGH,N))/(HHV(HIGH,N)-LLV(LOW,N))*100;//收盘价与N周期最高值做差，N周期最高值与N周期最低值做差，两差值间做比值。
	 * FAST:SMA(RSV,M1,1);//RSV的移动平均
	 * @return
	 */
	public Function<BarWrapper, TimeSeriesValue> fast() {
		final TimeSeriesUnaryOperator llv = LLV(this.n);
		final TimeSeriesUnaryOperator hhv = HHV(this.n);
		final TimeSeriesUnaryOperator sma = SMA(this.m1, 1);
		return bar -> {
			TimeSeriesValue lowV = llv.apply(new TimeSeriesValue(bar.getBar().getLowPrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			TimeSeriesValue highV = hhv.apply(new TimeSeriesValue(bar.getBar().getHighPrice(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
			double rsv = (bar.getBar().getClosePrice() - highV.getValue()) / (highV.getValue() - lowV.getValue()) * 100;
			return sma.apply(new TimeSeriesValue(rsv, bar.getBar().getActionTimestamp(), bar.isUnsettled()));
		};
	}
	
	/**
	 * 威廉慢线计算函数
	 * RSV:= (CLOSE-HHV(HIGH,N))/(HHV(HIGH,N)-LLV(LOW,N))*100;//收盘价与N周期最高值做差，N周期最高值与N周期最低值做差，两差值间做比值。
	 * FAST:SMA(RSV,M1,1);//RSV的移动平均
	 * SLOW:SMA(FAST,M2,1);//LWR1的移动平均
	 * @return
	 */
	public Function<BarWrapper, TimeSeriesValue> slow() {
		final Function<BarWrapper, TimeSeriesValue> fast = fast();
		final TimeSeriesUnaryOperator sma = SMA(this.m2, 1);
		return bar -> {
			TimeSeriesValue fastV = fast.apply(bar);
			return sma.apply(fastV);
		};
	}
}
