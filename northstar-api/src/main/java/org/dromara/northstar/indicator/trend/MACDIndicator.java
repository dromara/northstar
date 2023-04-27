package org.dromara.northstar.indicator.trend;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;
import org.dromara.northstar.indicator.helper.DiffIndicator;

/**
 * MACD指标
 * 算法：
 * DIFF : EMA(CLOSE,SHORT) - EMA(CLOSE,LONG);//短周期与长周期的收盘价的指数平滑移动平均值做差。
 * DEA  : EMA(DIFF,M);//DIFF的M个周期指数平滑移动平均
 * MACD : (DIFF - DEA) * 2
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
		return List.of(deaLine);	//依赖指标的更新在IndicatorValueUpdateHelper这个辅助类来处理，这里只需要提供正确的依赖关系。
	}
	
	@Override
	protected Num evaluate(Num num) {
		double val = (diffLine.value(0) - deaLine.value(0)) * 2;	//依赖的指标值会确保被优先更新，因此这里能直接使用
		return Num.of(val, num.unstable());
	}

}
