package org.dromara.northstar.indicator;

import java.util.Collections;
import java.util.List;

public class EMAIndicator extends AbstractMAIndicator implements Indicator{
	
	private Num lastVal;
	
	private double factor;
	
	public EMAIndicator(Configuration cfg, int barCount) {
		super(cfg, barCount);
		factor = 2D / (barCount + 1);	// EMA 的更新系数
	}

	@Override
	public List<Indicator> dependencies() {
		return Collections.emptyList();
	}

	@Override
	protected Num evaluate(Num num) {
		if(sample.size() == 0 || sample.size() == 1 && sample.get().unstable()) {	// 当计算样本没有值，或只有一个不稳定值时
			lastVal = num;
			return num;
		}
		double preVal = lastVal.unstable() ? sample.get(-1).value() : sample.get(0).value(); 
		double val = factor * num.value() + (1 - factor) * preVal;
		return Num.of(val, num.unstable());
	}

}
