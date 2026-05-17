package org.dromara.northstar.support.utils.bar;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.IndicatorValueUpdateHelper;
import org.dromara.northstar.module.ModuleContext;
import org.dromara.northstar.strategy.AbstractStrategy.BarHandler;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.MergedBarListener;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * K线合成中心
 * @author KevinHuangwl
 *
 */
public class BarMergerRegistry implements BarDataAware {

	private static final Set<MergedBarListener> EMPTY_SET = Collections.emptySet();
	
	private Table<Contract, Interval, Set<MergedBarListener>> indicatorListeners = HashBasedTable.create();
	private Table<Contract, Interval, Set<MergedBarListener>> contextListeners = HashBasedTable.create();
	private Table<Contract, Interval, Set<MergedBarListener>> handlerListeners = HashBasedTable.create();
	private Table<Contract, Interval, Set<MergedBarListener>> strategyListeners = HashBasedTable.create();
	
	private Map<ListenerType, Table<Contract, Interval, Set<MergedBarListener>>> listenTypeMap = new EnumMap<>(ListenerType.class);
	{
		listenTypeMap.put(ListenerType.INDICATOR, indicatorListeners);
		listenTypeMap.put(ListenerType.CONTEXT, contextListeners);
		listenTypeMap.put(ListenerType.HANDLER, handlerListeners);
		listenTypeMap.put(ListenerType.STRATEGY, strategyListeners);
	}
	
	private ModuleContext ctx;
	
	public BarMergerRegistry(ModuleContext ctx) {
		this.ctx = ctx;
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
		if(listenTypeMap.get(type).get(contract, interval) == null) {
			listenTypeMap.get(type).put(contract, interval, new HashSet<>());
		}
		listenTypeMap.get(type).get(contract, interval).add(listener);
	}
	
	@Override
	public void onBar(Bar bar) {
		Optional.ofNullable(indicatorListeners.get(bar.contract(), bar.interval())).orElse(EMPTY_SET)
			.forEach(listener -> listener.onMergedBar(bar));
		
		if(bar.unstable() || !ctx.isReady()) {
			return;	// 未收盘的K线数据不传递到后面的监听器
		}
		
		Optional.ofNullable(handlerListeners.get(bar.contract(), bar.interval())).orElse(EMPTY_SET)
			.forEach(listener -> listener.onMergedBar(bar));
		Optional.ofNullable(strategyListeners.get(bar.contract(), bar.interval())).orElse(EMPTY_SET)
			.forEach(listener -> listener.onMergedBar(bar));
		Optional.ofNullable(contextListeners.get(bar.contract(), bar.interval())).orElse(EMPTY_SET)
			.forEach(listener -> listener.onMergedBar(bar));
	}
	
	public enum ListenerType {
		INDICATOR,
		CONTEXT,
		HANDLER,
		STRATEGY;
	}

}
