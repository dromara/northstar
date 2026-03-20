package org.dromara.northstar.indicator;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.strategy.MergedBarListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndicatorValueUpdateHelper implements MergedBarListener {

	private final Indicator indicator;
	
	private final Configuration cfg;
	
	public IndicatorValueUpdateHelper(Indicator indicator) {
		this.indicator = indicator;
		this.cfg = indicator.getConfiguration();
	}
	
	@Override
	public void onMergedBar(Bar bar) {
		if(!StringUtils.equals(cfg.contract().unifiedSymbol(), bar.contract().unifiedSymbol())) {
			return;
		}
		recursiveUpdate(indicator, bar);
	}

	public Indicator getIndicator() {
		return indicator;
	}

	/*
	 * 递归更新
	 */
	private void recursiveUpdate(Indicator indicator, Bar bar) {
		for(Indicator dependencyIndicator: indicator.dependencies()) {
			recursiveUpdate(dependencyIndicator, bar);
		}
		try {			
			indicator.update(Num.of(indicator.getConfiguration().valueType().resolve(bar), bar.actionTimestamp(), bar.unstable()));
		} catch(Exception e) {
			log.error("指标[{}] 数值更新异常", indicator.getConfiguration().indicatorName(), e);
		}
	}
}
