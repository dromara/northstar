package org.dromara.northstar.support.utils.bar;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.strategy.MergedBarListener;

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
		listenTypeMap.put(ListenerType.CONTEXT, HashMultimap.create());
		listenTypeMap.put(ListenerType.STRATEGY, HashMultimap.create());
	}
	
	public void addListener(IContract contract, int numOfUnit, PeriodUnit unit, MergedBarListener listener, ListenerType type) {
		Identifier identifier = makeIdentifier(type, contract, numOfUnit, unit);
		BarMerger merger = mergerMap.get(identifier);
		if(merger == null) {
			merger = makeBarMerger(contract, numOfUnit, unit);
			mergerMap.put(identifier, merger);
		}
		listenTypeMap.get(type).put(merger, listener);
		mergerListenerMap.put(merger, listener);
	}
	
	private Identifier makeIdentifier(ListenerType type, IContract contract, int numOfUnit, PeriodUnit unit) {
		return Identifier.of(String.format("%s_%s_%d_%s", type, contract.contractField().getUnifiedSymbol(), numOfUnit, unit.symbol()));
	}
	
	private BarMerger makeBarMerger(IContract contract, int numOfUnit, PeriodUnit unit) {
		IDataSource dsMgr = contract.dataSource();
		return switch(unit) {
		case MINUTE -> new BarMerger(numOfUnit, contract, onMergedCallback);
		case HOUR -> new BarMerger(numOfUnit * 60, contract, onMergedCallback);
		case DAY -> new DailyBarMerger(numOfUnit, contract, onMergedCallback);
		case WEEK -> new WeeklyBarMerger(numOfUnit, contract, onMergedCallback, dsMgr);
		case MONTH -> new MonthlyBarMerger(numOfUnit, contract, onMergedCallback, dsMgr);
		default -> throw new IllegalArgumentException("Unexpected value: " + unit);
		};
	}

	@Override
	public void onBar(BarField bar) {
		listenTypeMap.get(ListenerType.INDICATOR).keySet().forEach(merger -> merger.onBar(bar));
		listenTypeMap.get(ListenerType.STRATEGY).keySet().forEach(merger -> merger.onBar(bar));
		listenTypeMap.get(ListenerType.CONTEXT).keySet().forEach(merger -> merger.onBar(bar));
	}
	
	public enum ListenerType {
		INDICATOR,
		CONTEXT,
		STRATEGY;
	}
}
