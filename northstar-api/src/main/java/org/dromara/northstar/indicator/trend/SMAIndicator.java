package org.dromara.northstar.indicator.trend;

import java.util.Objects;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;

/**
 * SMA扩展指数加权移动平均线
 * @author KevinHuangwl
 *
 */
public class SMAIndicator extends AbstractIndicator implements Indicator{

	private double factor;
	
	private Indicator srcIndicator;
	
	public SMAIndicator(Configuration cfg, int barCount, int weight) {
		super(cfg);
		this.factor = (double) weight / barCount;
	}
	
	public SMAIndicator(Configuration cfg, Indicator srcIndicator, int barCount, int weight) {
		this(cfg, barCount, weight);
		this.srcIndicator = srcIndicator;
	}

	@Override
	protected Num evaluate(Num num) {
		if(ringBuf.size() == 0 || ringBuf.size() == 1 && ringBuf.get().unstable()) {	// 当计算样本没有值，或只有一个不稳定值时
			return num;
		}
		double newVal = Objects.isNull(srcIndicator) ? num.value() : srcIndicator.value(0);
		double preVal = ringBuf.get().unstable() ? ringBuf.get(-1).value() : ringBuf.get(0).value(); 
		double val = factor * newVal + (1 - factor) * preVal;
		return Num.of(val, num.timestamp(), num.unstable());
	}

}
