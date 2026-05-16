package org.dromara.northstar.kline;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.strategy.MergedBarListener;

public class KlineGenerator {
	
	private BarMerger merger3m;
	private BarMerger merger5m;
	private BarMerger merger10m;
	private BarMerger merger15m;
	private BarMerger merger30m;
	private BarMerger merger1h;
	private BarMerger merger2h;
	private BarMerger merger4h;
	private DailyBarMerger merger1d;
	private WeeklyBarMerger merger1w;
	
	private FastEventEngine feEngine;
	
	private MergedBarListener listener = bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar);
	
	public KlineGenerator(Contract contract, FastEventEngine feEngine) {
		this.feEngine = feEngine;
		merger3m = new BarMerger(Interval.M3, contract);
		merger5m = new BarMerger(Interval.M5, contract);
		merger10m = new BarMerger(Interval.M10, contract);
		merger15m = new BarMerger(Interval.M15, contract);
		merger30m = new BarMerger(Interval.M30, contract);
		merger1h = new BarMerger(Interval.H1, contract);
		merger2h = new BarMerger(Interval.H2, contract);
		merger4h = new BarMerger(Interval.H4, contract);
		merger1d = new DailyBarMerger(contract);
		merger1w = new WeeklyBarMerger(contract);
		
		merger3m.addListener(listener);
		merger5m.addListener(listener);
		merger10m.addListener(listener);
		merger15m.addListener(listener);
		merger30m.addListener(listener);
		merger1h.addListener(listener);
		merger2h.addListener(listener);
		merger4h.addListener(listener);
		merger1d.addListener(listener);
		merger1w.addListener(listener);
	}
	
	public void updateTick(Tick tick) {
		if(tick.type() != TickType.MARKET_TICK && tick.type() != TickType.PLAYBACK_TICK) {
			throw new IllegalArgumentException("不接受此类型的Tick数据：" + tick.type());
		}
		
		merger3m.onTick(tick);
		merger5m.onTick(tick);
		merger10m.onTick(tick);
		merger15m.onTick(tick);
		merger30m.onTick(tick);
		merger1h.onTick(tick);
		merger2h.onTick(tick);
		merger4h.onTick(tick);
		merger1d.onTick(tick);
		merger1w.onTick(tick);
	}

	public void updateBar(Bar minuteBar) {
		if(minuteBar.interval() != Interval.M1) {
			throw new IllegalArgumentException("KlineGenerator只能接受1分钟线数据");
		}
		
		merger3m.onBar(minuteBar);
		merger5m.onBar(minuteBar);
		merger10m.onBar(minuteBar);
		merger15m.onBar(minuteBar);
		merger30m.onBar(minuteBar);
		merger1h.onBar(minuteBar);
		merger2h.onBar(minuteBar);
		merger4h.onBar(minuteBar);
		merger1d.onBar(minuteBar);
		merger1w.onBar(minuteBar);
	}
}
