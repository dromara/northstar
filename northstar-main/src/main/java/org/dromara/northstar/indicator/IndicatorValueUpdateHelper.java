package org.dromara.northstar.indicator;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.strategy.MergedBarListener;

public class IndicatorValueUpdateHelper implements MergedBarListener, BarDataAware, TickDataAware{

	private Indicator indicator;
	
	private Configuration cfg;
	
	private InstantBarGenerator inbarGen;
	
	public IndicatorValueUpdateHelper(Indicator indicator) {
		this.indicator = indicator;
		this.cfg = indicator.getConfiguration();
		this.inbarGen = new InstantBarGenerator(cfg.contract());
	}
	
	@Override
	public void onMergedBar(Bar bar) {
		if(!StringUtils.equals(cfg.contract().unifiedSymbol(), bar.contract().unifiedSymbol())) {
			return;
		}
		recursiveUpdate(indicator, bar, false);
	}

	@Override
	public void onBar(Bar bar) {
		if(!StringUtils.equals(cfg.contract().unifiedSymbol(), bar.contract().unifiedSymbol())) {
			return;
		}
		recursiveUpdate(indicator, bar, true);
	}

	@Override
	public void onTick(Tick tick) {
		if(!StringUtils.equals(cfg.contract().unifiedSymbol(), tick.contract().unifiedSymbol())) {
			return;
		}
		inbarGen.update(tick).ifPresent(this::onBar);
	}
	
	public Indicator getIndicator() {
		return indicator;
	}

	/*
	 * 递归更新
	 */
	private void recursiveUpdate(Indicator indicator, Bar bar, boolean unstable) {
		for(Indicator dependencyIndicator: indicator.dependencies()) {
			recursiveUpdate(dependencyIndicator, bar, unstable);
		}
		indicator.update(Num.of(indicator.getConfiguration().valueType().resolve(bar), bar.actionTimestamp(), unstable));
	}
}
