package org.dromara.northstar.indicator;

import java.util.Collections;
import java.util.List;

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
