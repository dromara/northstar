package org.dromara.northstar.indicator.volatility;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.helper.DiffIndicator;
import org.dromara.northstar.indicator.helper.StandardDeviationIndicator;
import org.dromara.northstar.indicator.helper.SumIndicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.indicator.trend.MAIndicator;

/**
 * 布林带指标
 * 
 * @author KevinHuangwl
 *
 */
public class BOLLIndicator extends AbstractIndicator implements Indicator{

	private static String ERR_MSG = "布林带指标是一个多值指标，不能直接调用该指标方法，应该获取具有的指标线进行调用";
	
	private Indicator mid;		// 中轨
	private Indicator std;		// 标准差
	private Indicator upper;	// 上轨
	private Indicator lower;	// 下轨
	
	public BOLLIndicator(Configuration cfg, int barCount, double multipler) {
		super(cfg.toBuilder().visible(false).build());
		mid = new MAIndicator(cfg.toBuilder().indicatorName("BOLL_mid").numOfUnits(barCount).build(), barCount);
		std = new StandardDeviationIndicator(cfg.toBuilder().indicatorName("BOLL_std").build(), barCount);
		upper = new SumIndicator(cfg.toBuilder().indicatorName("BOLL_upper").numOfUnits(barCount).build(), mid, 1, std, multipler);
		lower = new DiffIndicator(cfg.toBuilder().indicatorName("BOLL_lower").numOfUnits(barCount).build(), mid, 1, std, multipler);
	}
	
	@Override
	public List<Indicator> dependencies() {
		return List.of(mid, upper, lower);
	}

	/**
	 * 布林带的核心值本质上是计算标准差，其他均视为辅助值
	 * 所以此处计算的是标准差值
	 */
	@Override
	protected Num evaluate(Num num) {
		return std.get(0);
	}
	
	@Override
	public Num get(int step) {
		throw new UnsupportedOperationException(ERR_MSG);
	}

	@Override
	public double value(int step) {
		throw new UnsupportedOperationException(ERR_MSG);
	}

	@Override
	public List<Num> getData() {
		throw new UnsupportedOperationException(ERR_MSG);
	}

	public Indicator getMid() {
		return mid;
	}
	
	public Indicator getUpper() {
		return upper;
	}
	
	public Indicator getLower() {
		return lower;
	}
}
