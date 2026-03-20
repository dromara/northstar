package org.dromara.northstar.indicator.trend;

import java.util.List;

import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;

import lombok.Getter;

@Getter
public class PBXIndicator extends AbstractIndicator implements Indicator {

	private PuBuIndicator pb1;
	private PuBuIndicator pb2;
	private PuBuIndicator pb3;
	private PuBuIndicator pb4;
	private PuBuIndicator pb5;
	private PuBuIndicator pb6;
	
	public PBXIndicator(Configuration cfg) {
		super(cfg.toBuilder().visible(false).build());	// 由于PBX没有值，因此设置为不可见
		pb1 = new PuBuIndicator(cfg.toBuilder().indicatorName("PB1").build(), 4);
		pb2 = new PuBuIndicator(cfg.toBuilder().indicatorName("PB2").build(), 6);
		pb3 = new PuBuIndicator(cfg.toBuilder().indicatorName("PB3").build(), 9);
		pb4 = new PuBuIndicator(cfg.toBuilder().indicatorName("PB4").build(), 13);
		pb5 = new PuBuIndicator(cfg.toBuilder().indicatorName("PB5").build(), 18);
		pb6 = new PuBuIndicator(cfg.toBuilder().indicatorName("PB6").build(), 24);
	}
	
	@Override
	public List<Indicator> dependencies() {
		return List.of(pb1, pb2, pb3, pb4, pb5, pb6);
	}

	/**
	 * PBX本身没有值，依赖的是pb1-pb6的值
	 */
	@Override
	protected Num evaluate(Num num) {
		return Num.NaN();
	}

}
