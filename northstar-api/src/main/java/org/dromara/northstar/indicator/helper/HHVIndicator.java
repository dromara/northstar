package org.dromara.northstar.indicator.helper;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;

/**
 * 最大值指标
 * @author KevinHuangwl
 *
 */
public class HHVIndicator extends AbstractIndicator implements Indicator{

	private Indicator srcIndicator;
	
	public HHVIndicator(Configuration cfg, Indicator indicator) {
		super(cfg);
		this.srcIndicator = indicator;
	}
	
	@Override
	public List<Indicator> dependencies() {
		return List.of(srcIndicator);
	}

	@Override
	protected Num evaluate(Num num) {
		if(!srcIndicator.isReady()) {
			return Num.of(Double.NaN, 0, num.unstable());
		}
		double val = srcIndicator.getData().stream().filter(nm -> !nm.isNaN()).mapToDouble(Num::value).max().getAsDouble();
		return Num.of(val, num.timestamp(), num.unstable());
	}
	
	
}
