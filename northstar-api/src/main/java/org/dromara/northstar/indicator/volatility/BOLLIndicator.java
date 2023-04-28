package org.dromara.northstar.indicator.volatility;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;
import org.dromara.northstar.indicator.trend.MAIndicator;

/**
 * 布林带指标
 * 
 * @author KevinHuangwl
 *
 */
public class BOLLIndicator extends AbstractIndicator implements Indicator{

	private Indicator mid;		// 中轨
	private Indicator upper;	// 上轨
	private Indicator lower;	// 下轨
	
	public BOLLIndicator(Configuration cfg, int barCount, double multipler) {
		super(cfg);
		mid = new MAIndicator(cfg.toBuilder().indicatorName("BOLL_mid").numOfUnits(barCount).build(), barCount);
		upper = new BollingUpper(cfg.toBuilder().indicatorName("BOLL_upper").build(), barCount, multipler);
		lower = new BollingLower(cfg.toBuilder().indicatorName("BOLL_lower").build(), barCount, multipler);
	}
	
	@Override
	public List<Indicator> dependencies() {
		// 布林带指标的依赖关系非常特殊且典型
		// BOLL -> upper -> std -> mid
		// BOLL -> lower -> std -> mid
		// BOLL -> mid
		// 由于框架进行值更新时，会依次更新依赖的指标值，如果mid使用同一个MA指标，会导致同一个时间被更新三次
		// 因此，必须把它们的依赖值明确区分开，才能避免重复更新
		return List.of(mid, upper, lower);
	}

	/**
	 * 布林带的核心值本质上是计算标准差，其他均视为辅助值
	 * 所以此处计算的是标准差值
	 */
	@Override
	protected Num evaluate(Num num) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 为了使代码更清晰，对上轨进行单独的封装
	 * @author KevinHuangwl
	 *
	 */
	public class BollingUpper extends AbstractIndicator implements Indicator{
		
		public BollingUpper(Configuration cfg, int barCount, double multipler) {
			super(cfg);
		}

		@Override
		protected Num evaluate(Num num) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	/**
	 * 为了使代码更清晰，对下轨进行单独的封装
	 * @author KevinHuangwl
	 *
	 */
	public class BollingLower extends AbstractIndicator implements Indicator{
		
		public BollingLower(Configuration cfg, int barCount, double multipler) {
			super(cfg);
		}

		@Override
		protected Num evaluate(Num num) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
