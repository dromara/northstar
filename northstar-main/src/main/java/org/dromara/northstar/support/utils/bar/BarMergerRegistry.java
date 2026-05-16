package org.dromara.northstar.support.utils.bar;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.indicator.IndicatorValueUpdateHelper;
import org.dromara.northstar.kline.BarMerger;
import org.dromara.northstar.kline.BarMergerFactory;
import org.dromara.northstar.module.ModuleContext;
import org.dromara.northstar.strategy.AbstractStrategy.BarHandler;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.MergedBarListener;

/**
 * K线合成中心
 * @author KevinHuangwl
 *
 */
public class BarMergerRegistry implements BarDataAware, TickDataAware{
	
	protected Map<Identifier, BarMerger> mergerMap = new HashMap<>();
	protected Map<ListenerType, List<BarMerger>> listenTypeMap = new EnumMap<>(ListenerType.class);
	
	private ModuleContext ctx;
	
	public BarMergerRegistry(ModuleContext ctx) {
		this.ctx = ctx;
		listenTypeMap.put(ListenerType.INDICATOR, new ArrayList<>());
		listenTypeMap.put(ListenerType.CONTEXT, new ArrayList<>());
		listenTypeMap.put(ListenerType.HANDLER, new ArrayList<>());
		listenTypeMap.put(ListenerType.STRATEGY, new ArrayList<>());
	}
	
	public void addListener(Contract contract, Interval interval, MergedBarListener listener) {
		ListenerType type;
		if(listener instanceof IModuleContext) {
			type = ListenerType.CONTEXT;
		} else if(listener instanceof IndicatorValueUpdateHelper) {
			type = ListenerType.INDICATOR;
		} else if(listener instanceof BarHandler){
			type = ListenerType.HANDLER;
		} else {
			type = ListenerType.STRATEGY;
		}
		Identifier identifier = makeIdentifier(type, contract, interval);
		BarMerger merger = mergerMap.get(identifier);
		if(merger == null) {
			merger = BarMergerFactory.newInstance(interval, contract);
			mergerMap.put(identifier, merger);
		}
		listenTypeMap.get(type).add(merger);
		merger.addListener(listener);
	}
	
	private Identifier makeIdentifier(ListenerType type, Contract contract, Interval interval) {
		return Identifier.of(String.format("%s_%s_%s", type, contract.unifiedSymbol(), interval.getAlias()));
	}
	
	@Override
	public void onBar(Bar bar) {
		listenTypeMap.get(ListenerType.INDICATOR).forEach(merger -> merger.onBar(bar));
		listenTypeMap.get(ListenerType.HANDLER).forEach(merger -> merger.onBar(bar));
		if(ctx.isReady()) {			
			listenTypeMap.get(ListenerType.STRATEGY).forEach(merger -> merger.onBar(bar));
		}
		listenTypeMap.get(ListenerType.CONTEXT).forEach(merger -> merger.onBar(bar));
	}
	
	public enum ListenerType {
		INDICATOR,
		CONTEXT,
		HANDLER,
		STRATEGY;
	}

	@Override
	public void onTick(Tick tick) {
		listenTypeMap.get(ListenerType.INDICATOR).forEach(merger -> merger.onTick(tick));
	}
}
