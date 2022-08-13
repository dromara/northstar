package tech.quantit.northstar.strategy.api.indicator.complex;

import static tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions.EMA;

import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;

/**
 * DIFF : EMA(CLOSE,SHORT) - EMA(CLOSE,LONG);//短周期与长周期的收盘价的指数平滑移动平均值做差。
 * DEA  : EMA(DIFF,M);//DIFF的M个周期指数平滑移动平均
 * @author KevinHuangwl
 *
 */
public class MACD {

	private int fast;
	private int slow;
	private int m;
	
	/**
	 * 创建MACD指标线生成器
	 * @param fast	快线周期
	 * @param slow	慢线周期
	 * @param m		移动平均周期
	 */
	public MACD(int fast, int slow, int m) {
		this.fast = fast;
		this.slow = slow;
		this.m = m;
	}
	
	/**
	 * 创建MACD指标线生成器
	 * @param fast
	 * @param slow
	 * @param m
	 * @return
	 */
	public static MACD of(int fast, int slow, int m) {
		return new MACD(fast, slow, m);
	}
	
	/**
	 * 获取DIFF线计算函数
	 * DIFF : EMA(CLOSE,SHORT) - EMA(CLOSE,LONG);//短周期与长周期的收盘价的指数平滑移动平均值做差。
	 * @return
	 */
	public TimeSeriesUnaryOperator diff() {
		final TimeSeriesUnaryOperator fastLine = EMA(this.fast);
		final TimeSeriesUnaryOperator slowLine = EMA(this.slow);
		return tv -> {
			TimeSeriesValue v = fastLine.apply(tv);
			TimeSeriesValue v0 = slowLine.apply(tv);
			double val = v.getValue() - v0.getValue();
			return new TimeSeriesValue(val, tv.getTimestamp());
		};
	}
	
	/**
	 * 获取DEA线计算函数
	 * DEA  : EMA(DIFF,M);//DIFF的M个周期指数平滑移动平均
	 * @return
	 */
	public TimeSeriesUnaryOperator dea() {
		final TimeSeriesUnaryOperator fastLine = EMA(this.fast);
		final TimeSeriesUnaryOperator slowLine = EMA(this.slow);
		final TimeSeriesUnaryOperator ema = EMA(this.m);
		return tv -> {
			TimeSeriesValue v = fastLine.apply(tv);
			TimeSeriesValue v0 = slowLine.apply(tv);
			v.setValue(v.getValue() - v0.getValue());
			return ema.apply(v);
		};
	}
}
