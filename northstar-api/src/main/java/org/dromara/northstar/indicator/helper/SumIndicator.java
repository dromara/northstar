package org.dromara.northstar.indicator.helper;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;

/**
 * 求两指标值之和
 * @author KevinHuangwl
 *
 */
public class SumIndicator extends AbstractIndicator implements Indicator {

	private Indicator line1;
	private double multiplier1;
	private Indicator line2;
	private double multiplier2;
	
	public SumIndicator(Configuration cfg, Indicator line1, Indicator line2) {
		this(cfg, line1, 1, line2, 1);
	}
	
	public SumIndicator(Configuration cfg, Indicator line1, double multiplier1, Indicator line2, double multiplier2) {
		super(cfg);
		this.line1 = line1;
		this.line2 = line2;
		this.multiplier1 = multiplier1;
		this.multiplier2 = multiplier2;
	}
	
	protected Num evaluate(Num num) {
		return Num.of(line1.value(0) * multiplier1 + line2.value(0) * multiplier2, num.timestamp(), num.unstable());
	}

	@Override
	public List<Indicator> dependencies() {
		return List.of(line1, line2);
	}
}
