package org.dromara.northstar.support.utils.bar;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.IndicatorValueUpdateHelper;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.MergedBarListener;

/**
 * K线合成中心
 * @author KevinHuangwl
 *
 */
public class BarMergerRegistry implements BarDataAware{
	
	protected Map<Identifier, BarMerger> mergerMap = new HashMap<>();
	protected Map<ListenerType, Set<BarMerger>> listenTypeMap = new EnumMap<>(ListenerType.class);
	
	public BarMergerRegistry() {
		listenTypeMap.put(ListenerType.INDICATOR, new HashSet<>());
		listenTypeMap.put(ListenerType.CONTEXT, new HashSet<>());
		listenTypeMap.put(ListenerType.STRATEGY, new HashSet<>());
	}
	
	public void addListener(Contract contract, int numOfUnit, PeriodUnit unit, MergedBarListener listener) {
		ListenerType type;
		if(listener instanceof IModuleContext) {
			type = ListenerType.CONTEXT;
		} else if(listener instanceof IndicatorValueUpdateHelper) {
			type = ListenerType.INDICATOR;
		} else {
			type = ListenerType.STRATEGY;
		}
		Identifier identifier = makeIdentifier(type, contract, numOfUnit, unit);
		BarMerger merger = mergerMap.get(identifier);
		if(merger == null) {
			merger = makeBarMerger(contract, numOfUnit, unit);
			mergerMap.put(identifier, merger);
		}
		listenTypeMap.get(type).add(merger);
		merger.addListener(listener);
	}
	
	private Identifier makeIdentifier(ListenerType type, Contract contract, int numOfUnit, PeriodUnit unit) {
		return Identifier.of(String.format("%s_%s_%d_%s", type, contract.unifiedSymbol(), numOfUnit, unit.symbol()));
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
		listenTypeMap.get(ListenerType.INDICATOR).forEach(merger -> merger.onBar(bar));
		listenTypeMap.get(ListenerType.STRATEGY).forEach(merger -> merger.onBar(bar));
		listenTypeMap.get(ListenerType.CONTEXT).forEach(merger -> merger.onBar(bar));
	}
	
	
	public enum ListenerType {
		INDICATOR,
		CONTEXT,
		STRATEGY;
	}
}
