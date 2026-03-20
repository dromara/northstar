package org.dromara.northstar.indicator.trend;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;

/**
 * 瀑布线指标
 * @author KevinHuangwl
 *
 */
public class PuBuIndicator extends AbstractIndicator implements Indicator{
	
	private Indicator ema;
	private Indicator ma1;
	private Indicator ma2;
	
	public PuBuIndicator(Configuration cfg, int barCount) {
		super(cfg);
		ema = new EMAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_ema").visible(false).build(), barCount);
		ma1 = new MAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_ma1").visible(false).build(), barCount * 2);
		ma2 = new MAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_ma2").visible(false).build(), barCount * 4);
	}
	
	@Override
	public List<Indicator> dependencies() {
		return List.of(ema, ma1, ma2);
	}

	@Override
	protected Num evaluate(Num num) {
		if(!ema.isReady() || !ma1.isReady() || !ma2.isReady()) {
			return Num.NaN();
		}
		return Num.of((ema.value(0) + ma1.value(0) + ma2.value(0)) / 3, num.timestamp(), num.unstable());
	}

}
