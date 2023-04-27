package org.dromara.northstar.indicator.trend;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;

/**
 * EMA指数加权平均线
 * @author KevinHuangwl
 *
 */
public class EMAIndicator extends AbstractIndicator implements Indicator{
	
	private double factor;
	
	private Indicator srcIndicator;
	
	public EMAIndicator(Configuration cfg, int barCount) {
		super(cfg);
		this.factor = 2D / (barCount + 1);	// EMA 的更新系数
	}
	
	public EMAIndicator(Configuration cfg, Indicator indicator, int barCount) {
		this(cfg, barCount);
		this.srcIndicator = indicator;
	}
	
	@Override
	public List<Indicator> dependencies() {
		if(Objects.isNull(srcIndicator)) {
			return Collections.emptyList();
		}
		return List.of(srcIndicator);
	}

	protected Num evaluate(Num num) {
		if(ringBuf.size() == 0 || ringBuf.size() == 1 && ringBuf.get().unstable()) {	// 当计算样本没有值，或只有一个不稳定值时
			return num;
		}
		double newVal = Objects.isNull(srcIndicator) ? num.value() : srcIndicator.value(0);
		double preVal = ringBuf.get().unstable() ? ringBuf.get(-1).value() : ringBuf.get(0).value(); 
		double val = factor * newVal + (1 - factor) * preVal;
		return Num.of(val, num.unstable());
	}

}
