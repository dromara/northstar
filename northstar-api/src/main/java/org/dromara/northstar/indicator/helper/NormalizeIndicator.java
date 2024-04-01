package org.dromara.northstar.indicator.helper;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;

/**
 * 对源指标的计算值进行归一化处理
 * @author KevinHuangwl
 *
 */
public class NormalizeIndicator extends AbstractIndicator implements Indicator{

	private Indicator srcIndicator;
	
	public NormalizeIndicator(Configuration cfg, Indicator srcIndicator) {
		super(cfg);
		this.srcIndicator = srcIndicator;
	}

	@Override
	public List<Indicator> dependencies() {
		return List.of(srcIndicator);
	}

	@Override
	protected Num evaluate(Num num) {
		if(!srcIndicator.isReady() || num.unstable()) {
			return Num.NaN();
		}
		StandardDeviation std = new StandardDeviation();
		double[] data = srcIndicator.getData().stream().mapToDouble(Num::value).toArray();
		double stdVal = std.evaluate(data, 0);
		return Num.of(srcIndicator.value(0) / stdVal, num.timestamp());
	}
	
}
