package org.dromara.northstar.indicator;

import org.apache.commons.codec.binary.StringUtils;
import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.strategy.MergedBarListener;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

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
	public void onMergedBar(BarField bar) {
		if(!StringUtils.equals(cfg.contract().getUnifiedSymbol(), bar.getUnifiedSymbol())) {
			return;
		}
		recursiveUpdate(indicator, Num.of(cfg.valueType().resolve(bar), bar.getActionTimestamp()));
	}

	@Override
	public void onBar(BarField bar) {
		if(!StringUtils.equals(cfg.contract().getUnifiedSymbol(), bar.getUnifiedSymbol())) {
			return;
		}
		recursiveUpdate(indicator, Num.of(cfg.valueType().resolve(bar), bar.getActionTimestamp(), true));
	}

	@Override
	public void onTick(TickField tick) {
		if(!StringUtils.equals(cfg.contract().getUnifiedSymbol(), tick.getUnifiedSymbol())) {
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
	private void recursiveUpdate(Indicator indicator, Num num) {
		for(Indicator dependencyIndicator: indicator.dependencies()) {
			recursiveUpdate(dependencyIndicator, num);
		}
		indicator.update(num);
	}
}
