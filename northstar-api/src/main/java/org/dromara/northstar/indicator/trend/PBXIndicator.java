package org.dromara.northstar.indicator.trend;

import java.util.List;

import org.dromara.northstar.common.constant.IndicatorPlacement;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Contract;
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
	
	public PBXIndicator(Contract c, Interval interval, int param1, int param2, int param3, int param4, int param5, int param6) {
		Configuration base = Configuration.builder().contract(c).interval(interval).group("PBX").placement(IndicatorPlacement.MAIN).build();
		super(base.toBuilder().visible(false).build());	// 由于PBX没有值，因此设置为不可见
		pb1 = new PuBuIndicator(base.toBuilder().indicatorName("PB1").build(), param1);
		pb2 = new PuBuIndicator(base.toBuilder().indicatorName("PB2").build(), param2);
		pb3 = new PuBuIndicator(base.toBuilder().indicatorName("PB3").build(), param3);
		pb4 = new PuBuIndicator(base.toBuilder().indicatorName("PB4").build(), param4);
		pb5 = new PuBuIndicator(base.toBuilder().indicatorName("PB5").build(), param5);
		pb6 = new PuBuIndicator(base.toBuilder().indicatorName("PB6").build(), param6);
	}
	
	public PBXIndicator(Contract c, Interval interval) {
		this(c, interval, 4, 6, 9, 13, 18, 24);
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

	private class PuBuIndicator extends AbstractIndicator implements Indicator{
		
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
	
}
