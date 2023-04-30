package org.dromara.northstar.indicator.trend;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;
import org.dromara.northstar.indicator.helper.DiffIndicator;
import org.springframework.util.Assert;

/**
 * MACD指标
 * 算法：
 * DIFF : EMA(CLOSE,SHORT) - EMA(CLOSE,LONG);//短周期与长周期的收盘价的指数平滑移动平均值做差。
 * DEA  : EMA(DIFF,M);//DIFF的M个周期指数平滑移动平均
 * MACD : (DIFF - DEA) * 2
 * 
 * MACD是一个多值指标，MACD值本身是指MACD柱，但在同一周期下还存在dea与diff值。
 * 本案例展示了如何管理dea与diff两个辅助值
 * 
 * @author KevinHuangwl
 *
 */
public class MACDIndicator extends AbstractIndicator implements Indicator{
	
	protected Indicator diffLine;
	protected Indicator deaLine;

	/**
	 * 求默认的MACD值
	 * @param cfg
	 * @param shortBarCount
	 * @param longBarCount
	 * @param signalBarCount
	 */
	public MACDIndicator(Configuration cfg, int shortBarCount, int longBarCount, int signalBarCount) {
		super(cfg);
		Assert.isTrue(shortBarCount < longBarCount, "长短周期设置不正确");
		Indicator shortTermLine = new EMAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_fast").visible(false).build(), shortBarCount);
		Indicator longTermLine = new EMAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_slow").visible(false).build(), longBarCount);
		
		diffLine = new DiffIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_diff").build(), shortTermLine, longTermLine);
		deaLine = new EMAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_dea").build(), diffLine, signalBarCount);
	}
	
	/**
	 * 求任意两个指标线的MACD值
	 * @param cfg
	 * @param fastLine
	 * @param slowLine
	 * @param signalBarCount
	 */
	public MACDIndicator(Configuration cfg, Indicator fastLine, Indicator slowLine, int signalBarCount) {
		super(cfg);
		
		diffLine = new DiffIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_diff").build(), fastLine, slowLine);
		deaLine = new EMAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_dea").build(), diffLine, signalBarCount);
	}
	
	@Override
	public List<Indicator> dependencies() {
		// 依赖指标的更新在IndicatorValueUpdateHelper这个辅助类来处理，这里只需要提供正确的依赖关系。
		// 它们的依赖关系是，MACD -> deaLine -> diffLine -> (shortTermLine, longTermLine)
		// 对于每一层，只需要指定下一层的依赖。框架更新时，会自动做递归处理
		return List.of(deaLine);	
	}
	
	/**
	 * 由于MACD属于多值指标，但可以把MACD红绿柱本身看作核心值，dea与diff看作辅助值，
	 * 所以此处的核心运算是MACD红绿柱值的运算
	 */
	@Override
	protected Num evaluate(Num num) {
		double val = (diffLine.value(0) - deaLine.value(0)) * 2;	//依赖的指标值会确保被优先更新，因此这里能直接使用
		return Num.of(val, num.timestamp(), num.unstable());
	}

	public Indicator getDiffLine() {
		return diffLine;
	}
	
	public Indicator getDeaLine() {
		return deaLine;
	}
}
