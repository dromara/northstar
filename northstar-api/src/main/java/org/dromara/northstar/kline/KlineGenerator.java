package org.dromara.northstar.kline;

import java.util.EnumMap;
import java.util.Map;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.strategy.MergedBarListener;

public class KlineGenerator {
	
	private MinuteBarGenerator minBarGen;
	
	private Map<Interval, BarMerger> mergerMap = new EnumMap<>(Interval.class);
	
	private final MergedBarListener listener;
	
	private final Contract contract;
	
	public KlineGenerator(Contract contract, MergedBarListener listener) {
		minBarGen = new MinuteBarGenerator(contract, this::updateBar);
		mergerMap.put(Interval.M1, BarMergerFactory.newInstance(Interval.M1, contract));
		mergerMap.put(Interval.M3, BarMergerFactory.newInstance(Interval.M3, contract));
		mergerMap.put(Interval.M5, BarMergerFactory.newInstance(Interval.M5, contract));
		mergerMap.put(Interval.M10, BarMergerFactory.newInstance(Interval.M10, contract));
		mergerMap.put(Interval.M15, BarMergerFactory.newInstance(Interval.M15, contract));
		mergerMap.put(Interval.M30, BarMergerFactory.newInstance(Interval.M30, contract));
		mergerMap.put(Interval.H1, BarMergerFactory.newInstance(Interval.H1, contract));
		mergerMap.put(Interval.H2, BarMergerFactory.newInstance(Interval.H2, contract));
		mergerMap.put(Interval.H4, BarMergerFactory.newInstance(Interval.H4, contract));
		mergerMap.put(Interval.D1, BarMergerFactory.newInstance(Interval.D1, contract));
		mergerMap.put(Interval.WK1, BarMergerFactory.newInstance(Interval.WK1, contract));
		
		setListener(listener);
		this.listener = listener;
		this.contract = contract;
	}
	
	public void updateTick(Tick tick) {
		if(tick.type() != TickType.MARKET_TICK && tick.type() != TickType.PLAYBACK_TICK) {
			throw new IllegalArgumentException("不接受此类型的Tick数据：" + tick.type());
		}
		
		minBarGen.update(tick);
		mergerMap.get(Interval.M1).onTick(tick);
		mergerMap.get(Interval.M3).onTick(tick);
		mergerMap.get(Interval.M5).onTick(tick);
		mergerMap.get(Interval.M10).onTick(tick);
		mergerMap.get(Interval.M15).onTick(tick);
		mergerMap.get(Interval.M30).onTick(tick);
		mergerMap.get(Interval.H1).onTick(tick);
		mergerMap.get(Interval.H2).onTick(tick);
		mergerMap.get(Interval.H4).onTick(tick);
		mergerMap.get(Interval.D1).onTick(tick);
		mergerMap.get(Interval.WK1).onTick(tick);
		
	}

	public void updateBar(Bar minuteBar) {
		mergerMap.get(Interval.M1).onBar(minuteBar);
		mergerMap.get(Interval.M3).onBar(minuteBar);
		mergerMap.get(Interval.M5).onBar(minuteBar);
		mergerMap.get(Interval.M10).onBar(minuteBar);
		mergerMap.get(Interval.M15).onBar(minuteBar);
		mergerMap.get(Interval.M30).onBar(minuteBar);
		mergerMap.get(Interval.H1).onBar(minuteBar);
		mergerMap.get(Interval.H2).onBar(minuteBar);
		mergerMap.get(Interval.H4).onBar(minuteBar);
		mergerMap.get(Interval.D1).onBar(minuteBar);
		mergerMap.get(Interval.WK1).onBar(minuteBar);
	}
	
	public void setListener(MergedBarListener listener) {
		mergerMap.get(Interval.M1).addListener(listener);
		mergerMap.get(Interval.M3).addListener(listener);
		mergerMap.get(Interval.M5).addListener(listener);
		mergerMap.get(Interval.M10).addListener(listener);
		mergerMap.get(Interval.M15).addListener(listener);
		mergerMap.get(Interval.M30).addListener(listener);
		mergerMap.get(Interval.H1).addListener(listener);
		mergerMap.get(Interval.H2).addListener(listener);
		mergerMap.get(Interval.H4).addListener(listener);
		mergerMap.get(Interval.D1).addListener(listener);
		mergerMap.get(Interval.WK1).addListener(listener);
	}
	
	public BarMerger getBarMerger(Interval interval) {
		return mergerMap.get(interval);
	}
	
	public synchronized void reset(Interval interval) {
		mergerMap.put(interval, BarMergerFactory.newInstance(interval, contract));
		mergerMap.get(interval).addListener(listener);
	}
}
