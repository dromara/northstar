package org.dromara.northstar.indicator.trend;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;
import org.dromara.northstar.indicator.RingArray;

/**
 * MA算术平均线
 * @author KevinHuangwl
 *
 */
public class MAIndicator extends AbstractIndicator implements Indicator {

	private RingArray<Num> sample;
	
	private double sum;
	
	public MAIndicator(Configuration cfg, int barCount) {
		super(cfg);
		sample = new RingArray<>(barCount);
	}

	protected Num evaluate(Num num) {
		sample.update(num, num.unstable()).ifPresent(val -> sum -= val.value());	// 减去旧的值
		sum += num.value();															// 加上新的值
		return Num.of(sum / sample.size(), num.unstable());
	}
	
}
