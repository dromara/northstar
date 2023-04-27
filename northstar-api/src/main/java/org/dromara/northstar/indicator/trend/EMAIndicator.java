package org.dromara.northstar.indicator.trend;

import java.util.Collections;
import java.util.List;

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
	
	public EMAIndicator(Configuration cfg, int barCount) {
		super(cfg);
		this.factor = 2D / (barCount + 1);	// EMA 的更新系数
	}

	@Override
	public List<Indicator> dependencies() {
		return Collections.emptyList();
	}
	
	@Override
	public void update(Num num) {
		super.update(evaluate(num));
	}

	private Num evaluate(Num num) {
		if(ringBuf.size() == 0 || ringBuf.size() == 1 && ringBuf.get().unstable()) {	// 当计算样本没有值，或只有一个不稳定值时
			return num;
		}
		double preVal = ringBuf.get().unstable() ? ringBuf.get(-1).value() : ringBuf.get(0).value(); 
		double val = factor * num.value() + (1 - factor) * preVal;
		return Num.of(val, num.unstable());
	}

}
