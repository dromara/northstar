package org.dromara.northstar.indicator.helper;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Configuration;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.Num;
import org.dromara.northstar.indicator.RingArray;

public class StandardDeviationIndicator extends AbstractIndicator implements Indicator {

	private Indicator srcIndicator;
	
	private RingArray<Num> sample;
	
	public StandardDeviationIndicator(Configuration cfg, int barCount) {
		super(cfg);
		this.sample = new RingArray<>(barCount);
	}
	
	public StandardDeviationIndicator(Configuration cfg, Indicator indicator, int barCount) {
		this(cfg, barCount);
		this.srcIndicator = indicator;
	}
	
	@Override
	public void update(Num num) {
		// TODO Auto-generated method stub
		super.update(num);
	}

	@Override
	public List<Indicator> dependencies() {
		// TODO Auto-generated method stub
		return super.dependencies();
	}

	@Override
	protected Num evaluate(Num num) {
		
		return null;
	}
}
