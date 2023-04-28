package org.dromara.northstar.indicator.trend;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
	
	private Indicator srcIndicator;
	
	public MAIndicator(Configuration cfg, int barCount) {
		super(cfg);
		sample = new RingArray<>(barCount);
	}
	
	public MAIndicator(Configuration cfg, Indicator srcIndicator, int barCount) {
		this(cfg, barCount);
		this.srcIndicator = srcIndicator;
	}

	protected Num evaluate(Num num) {
		Num newVal = Objects.isNull(srcIndicator) ? num : srcIndicator.get(0);
		sample.update(newVal, newVal.unstable()).ifPresent(val -> sum -= val.value());	// 减去旧的值
		sum += newVal.value();															// 加上新的值
		return Num.of(sum / sample.size(), num.timestamp(), num.unstable());
	}
	
	@Override
	public List<Indicator> dependencies() {
		if(Objects.isNull(srcIndicator)) {
			return Collections.emptyList();
		}
		return List.of(srcIndicator);
	}
	
}
