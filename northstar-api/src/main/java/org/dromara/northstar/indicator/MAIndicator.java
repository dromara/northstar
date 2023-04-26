package org.dromara.northstar.indicator;

import java.util.Collections;
import java.util.List;

public class MAIndicator extends AbstractIndicator implements Indicator {

	private RingArray<Num> sample;
	
	private double sum;
	
	public MAIndicator(Configuration cfg, int barCount) {
		super(cfg);
		sample = new RingArray<>(barCount);
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
		sample.update(num, num.unstable()).ifPresent(val -> sum -= val.value());	// 减去旧的值
		sum += num.value();															// 加上新的值
		return Num.of(sum / sample.size(), num.unstable());
	}
	
}
