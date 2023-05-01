package org.dromara.northstar.indicator.helper;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;

/**
 * 最小值指标
 * @author KevinHuangwl
 *
 */
public class LLVIndicator extends AbstractIndicator implements Indicator{

	private Indicator srcIndicator;
	
	public LLVIndicator(Configuration cfg, Indicator indicator) {
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
			return Num.NaN();
		}
		double val = srcIndicator.getData().stream().filter(nm -> !nm.isNaN()).mapToDouble(Num::value).min().getAsDouble();
		return Num.of(val, num.timestamp(), num.unstable());
	}
	
}
