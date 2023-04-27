package org.dromara.northstar.indicator.helper;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;

/**
 * 求两指标值之差
 * @author KevinHuangwl
 *
 */
public class DiffIndicator extends AbstractIndicator implements Indicator {

	private Indicator line1;
	private Indicator line2;
	
	public DiffIndicator(Configuration cfg, Indicator line1, Indicator line2) {
		super(cfg);
		this.line1 = line1;
		this.line2 = line2;
	}

	protected Num evaluate(Num num) {
		return Num.of(line1.value(0) - line2.value(0), num.unstable());
	}

	@Override
	public List<Indicator> dependencies() {
		return List.of(line1, line2);
	}
}
