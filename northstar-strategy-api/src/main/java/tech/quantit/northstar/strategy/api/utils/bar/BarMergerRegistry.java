package tech.quantit.northstar.strategy.api.utils.bar;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.common.collect.HashMultimap;

import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.strategy.api.BarDataAware;
import tech.quantit.northstar.strategy.api.MergedBarListener;
import tech.quantit.northstar.strategy.api.indicator.Indicator.PeriodUnit;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * K线合成中心
 * @author KevinHuangwl
 *
 */
public class BarMergerRegistry implements BarDataAware{

	protected Map<Identifier, BarMerger> mergerMap = new HashMap<>();
	protected Map<CallbackPriority, HashMultimap<BarMerger, MergedBarListener>> prioritizedListenerMap = new EnumMap<>(CallbackPriority.class);
	
	private BiConsumer<BarMerger, BarField> onMergedCallback = (merger, bar) -> 
		prioritizedListenerMap.entrySet().forEach(e -> 
			e.getValue().get(merger).forEach(listener -> listener.onMergedBar(bar))
		);
	
	public BarMergerRegistry() {
		prioritizedListenerMap.put(CallbackPriority.ONE, HashMultimap.create());
		prioritizedListenerMap.put(CallbackPriority.TWO, HashMultimap.create());
		prioritizedListenerMap.put(CallbackPriority.THREE, HashMultimap.create());
		prioritizedListenerMap.put(CallbackPriority.FOUR, HashMultimap.create());
	}
	
	public void addListener(Contract contract, int numOfUnit, PeriodUnit unit, MergedBarListener listener, CallbackPriority priority) {
		Identifier identifier = makeIdentifier(contract, numOfUnit, unit);
		mergerMap.computeIfAbsent(identifier, k -> makeBarMerger(contract, numOfUnit, unit));
		prioritizedListenerMap.get(priority).put(mergerMap.get(identifier), listener);
	}
	
	private Identifier makeIdentifier(Contract contract, int numOfUnit, PeriodUnit unit) {
		return Identifier.of(String.format("%s_%d_%s", contract.contractField().getUnifiedSymbol(), numOfUnit, unit.symbol()));
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
		prioritizedListenerMap.forEach((priority, map) -> 
			map.keySet().forEach(merger -> merger.onBar(bar))
		);
	}
	
	public enum CallbackPriority {
		ONE,
		TWO,
		THREE,
		FOUR;
	}
}
