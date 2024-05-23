package org.dromara.northstar.indicator.helper;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;

/**
 * 简单值指标
 * @author KevinHuangwl
 *
 */
public class SimpleValueIndicator extends AbstractIndicator implements Indicator {

	public SimpleValueIndicator(Configuration cfg) {
		super(cfg);
	}

	@Override
	protected Num evaluate(Num num) {
		return num;
	}

	@Override
	public boolean isReady() {
		return true;
	}
	
}
