package org.dromara.northstar.support.utils.bar;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.strategy.MergedBarListener;
import org.dromara.northstar.strategy.constant.PeriodUnit;

import com.google.common.collect.HashMultimap;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * K线合成中心
 * @author KevinHuangwl
 *
 */
public class BarMergerRegistry implements BarDataAware{
	
	private Map<Identifier, BarMerger> mergerMap = new HashMap<>();

	protected Map<ListenerType, HashMultimap<BarMerger, MergedBarListener>> listenTypeMap = new EnumMap<>(ListenerType.class);
	
	protected HashMultimap<BarMerger, MergedBarListener> mergerListenerMap = HashMultimap.create();
	
	private BiConsumer<BarMerger, BarField> onMergedCallback = (merger, bar) -> 
		mergerListenerMap.get(merger).forEach(listener -> listener.onMergedBar(bar));
	
	public BarMergerRegistry() {
		listenTypeMap.put(ListenerType.INDICATOR, HashMultimap.create());
		listenTypeMap.put(ListenerType.COMBO_INDICATOR, HashMultimap.create());
		listenTypeMap.put(ListenerType.CONTEXT, HashMultimap.create());
		listenTypeMap.put(ListenerType.INSPECTABLE_VAL, HashMultimap.create());
		listenTypeMap.put(ListenerType.STRATEGY, HashMultimap.create());
	}
	
	public void addListener(Contract contract, int numOfUnit, PeriodUnit unit, MergedBarListener listener, ListenerType type) {
		Identifier identifier = makeIdentifier(type, contract, numOfUnit, unit);
		BarMerger merger = mergerMap.get(identifier);
		if(merger == null) {
			merger = makeBarMerger(contract, numOfUnit, unit);
			mergerMap.put(identifier, merger);
		}
		listenTypeMap.get(type).put(merger, listener);
		mergerListenerMap.put(merger, listener);
	}
	
	private Identifier makeIdentifier(ListenerType type, Contract contract, int numOfUnit, PeriodUnit unit) {
		return Identifier.of(String.format("%s_%s_%d_%s", type, contract.contractField().getUnifiedSymbol(), numOfUnit, unit.symbol()));
	}
	
	private BarMerger makeBarMerger(Contract contract, int numOfUnit, PeriodUnit unit) {
		return switch(unit) {
		case MINUTE -> new BarMerger(numOfUnit, contract, onMergedCallback);
		case HOUR -> new BarMerger(numOfUnit * 60, contract, onMergedCallback);
		case DAY -> new DailyBarMerger(numOfUnit, contract, onMergedCallback);
		case WEEK -> new WeeklyBarMerger(numOfUnit, contract, onMergedCallback);
		case MONTH -> new MonthlyBarMerger(numOfUnit, contract, onMergedCallback);
		default -> throw new IllegalArgumentException("Unexpected value: " + unit);
		};
	}

	@Override
	public void onBar(BarField bar) {
		listenTypeMap.get(ListenerType.INDICATOR).keySet().forEach(merger -> merger.onBar(bar));
		listenTypeMap.get(ListenerType.COMBO_INDICATOR).keySet().forEach(merger -> merger.onBar(bar));
		listenTypeMap.get(ListenerType.STRATEGY).keySet().forEach(merger -> merger.onBar(bar));
		listenTypeMap.get(ListenerType.INSPECTABLE_VAL).keySet().forEach(merger -> merger.onBar(bar));
		listenTypeMap.get(ListenerType.CONTEXT).keySet().forEach(merger -> merger.onBar(bar));
	}
	
	public enum ListenerType {
		INDICATOR,
		COMBO_INDICATOR,
		CONTEXT,
		INSPECTABLE_VAL,
		STRATEGY;
	}
}
