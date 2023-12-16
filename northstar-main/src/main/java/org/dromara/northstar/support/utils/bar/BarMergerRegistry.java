package org.dromara.northstar.support.utils.bar;

import java.util.HashMap;
import java.util.Map;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.strategy.MergedBarListener;

/**
 * K线合成中心
 * @author KevinHuangwl
 *
 */
public class BarMergerRegistry implements BarDataAware{
	
	protected Map<Identifier, BarMerger> mergerMap = new HashMap<>();

	public void addListener(Contract contract, int numOfUnit, PeriodUnit unit, MergedBarListener listener) {
		Identifier identifier = makeIdentifier(contract, numOfUnit, unit);
		BarMerger merger = mergerMap.get(identifier);
		if(merger == null) {
			merger = makeBarMerger(contract, numOfUnit, unit);
			mergerMap.put(identifier, merger);
		}
		merger.addListener(listener);
	}
	
	private Identifier makeIdentifier(Contract contract, int numOfUnit, PeriodUnit unit) {
		return Identifier.of(String.format("%s_%d_%s", contract.unifiedSymbol(), numOfUnit, unit.symbol()));
	}
	
	private BarMerger makeBarMerger(Contract contract, int numOfUnit, PeriodUnit unit) {
		return switch(unit) {
		case MINUTE -> new BarMerger(numOfUnit, contract);
		case HOUR -> new BarMerger(numOfUnit * 60, contract);
		case DAY -> new DailyBarMerger(numOfUnit, contract);
		default -> throw new IllegalArgumentException("Unexpected value: " + unit);
		};
	}

	@Override
	public void onBar(Bar bar) {
		mergerMap.values().forEach(merger -> merger.onBar(bar));
	}
	
	public enum ListenerType {
		INDICATOR,
		CONTEXT,
		STRATEGY;
	}
}
