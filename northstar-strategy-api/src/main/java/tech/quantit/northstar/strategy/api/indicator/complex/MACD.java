package tech.quantit.northstar.strategy.api.indicator.complex;

import static tech.quantit.northstar.strategy.api.indicator.function.AverageFunctions.EMA;

import java.util.function.Function;

import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import tech.quantit.northstar.strategy.api.indicator.function.ComputeFunctions;
import xyz.redtorch.pb.CoreField.BarField;

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
		return diff(EMA(this.fast), EMA(this.slow));
	}
	
	/**
	 * 获取DEA线计算函数
	 * DEA  : EMA(DIFF,M);//DIFF的M个周期指数平滑移动平均
	 * @return
	 */
	public TimeSeriesUnaryOperator dea() {
		return dea(EMA(this.fast), EMA(this.slow), this.m);
	}
	
	/**
	 * 获取DIFF线计算函数
	 * @param fastLine		快线计算函数 
	 * @param slowLine		慢线计算函数
	 * @return
	 */
	public static TimeSeriesUnaryOperator diff(TimeSeriesUnaryOperator fastLine, TimeSeriesUnaryOperator slowLine) {
		return ComputeFunctions.minus(fastLine, slowLine);
	}
	
	/**
	 * 获取DEA线计算函数
	 * @param fastLine		快线计算函数 
	 * @param slowLine		慢线计算函数
	 * @return
	 */
	public static TimeSeriesUnaryOperator dea(TimeSeriesUnaryOperator fastLine, TimeSeriesUnaryOperator slowLine, int m) {
		return diff(fastLine, slowLine).andThen(EMA(m));
	}
	
	/**
	 * 获取MACD红绿柱计算函数
	 * @param diff
	 * @param dea
	 * @return
	 */
	public static TimeSeriesUnaryOperator post(TimeSeriesUnaryOperator diff, TimeSeriesUnaryOperator dea) {
		return tv -> {
			TimeSeriesValue difVal = diff.apply(tv);
			TimeSeriesValue deaVal = dea.apply(tv);
			return new TimeSeriesValue(2 * (difVal.getValue() - deaVal.getValue()), tv.getTimestamp());
		};
	}
	
	/**
	 * 获取DIFF线计算函数
	 * @param fastLine		快线计算函数 
	 * @param slowLine		慢线计算函数
	 * @return
	 */
	public static Function<BarField, TimeSeriesValue> diff(Function<BarField, TimeSeriesValue> fastLine, Function<BarField, TimeSeriesValue> slowLine) {
		return ComputeFunctions.diff(fastLine, slowLine);
	}
	
	/**
	 * 获取DEA线计算函数
	 * @param fastLine		快线计算函数 
	 * @param slowLine		慢线计算函数
	 * @return
	 */
	public static Function<BarField, TimeSeriesValue> dea(Function<BarField, TimeSeriesValue> fastLine, Function<BarField, TimeSeriesValue> slowLine, int m) {
		return diff(fastLine, slowLine).andThen(EMA(m));
	}
	
	/**
	 * 获取MACD红绿柱计算函数
	 * @param diff
	 * @param dea
	 * @return
	 */
	public static Function<BarField, TimeSeriesValue> post(Function<BarField, TimeSeriesValue> diff, Function<BarField, TimeSeriesValue> dea){
		return bar -> {
			TimeSeriesValue difVal = diff.apply(bar);
			TimeSeriesValue deaVal = dea.apply(bar);
			return new TimeSeriesValue(2 * (difVal.getValue() - deaVal.getValue()), bar.getActionTimestamp());
		};
	}
}
