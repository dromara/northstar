package org.dromara.northstar.kline;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.strategy.MergedBarListener;

import lombok.extern.slf4j.Slf4j;

/**
 * 分钟线合成器
 * @author KevinHuangwl
 *
 * 整个Merger的合成思路是：
 * 把 TickTime, BarTime （统一标准化为）-> MinuteEntry（分钟格）然后找到每个entry对应的cutoffTime -> cutoffTime  计得-> cutoffDateTime 
 */
@Slf4j
public class BarMerger implements BarDataAware, TickDataAware {
	
	protected final Contract contract;
	
	protected long curBarTimestamp;
	
	private LocalDateTime cutoffDateTime;
	
	protected Bar protoBar;
	
	protected final Set<MergedBarListener> listeners = new HashSet<>();
	
	protected int volDelta;
	protected int oiDelta;
	protected double turnoverDelta;
	
	protected int barVolDelta;
	protected int barOiDelta;
	protected double barTurnoverDelta;

	protected double high = Double.MIN_VALUE;
	protected double low = Double.MAX_VALUE;
	
	protected BarTimePadding barTimePadding;
	private Interval barInterval;
	
	public BarMerger(Interval interval, Contract contract) { 
		this.contract = contract;
		this.barInterval = interval;
		this.barTimePadding = new BarTimePadding(interval, contract);
	}

	@Override
	public synchronized void onBar(Bar bar) {
		if(!contract.equals(bar.contract()) || !barTimePadding.valid(bar)) {
			if(log.isTraceEnabled()) {				
				log.trace("[{}] K线合成器忽略Bar数据 [{}]", contract.unifiedSymbol(), String.format("%s:%s:%s", bar.contract().unifiedSymbol(), bar.actionDay(), bar.actionTime()));
			}
			return;
		}
		
		LocalDateTime barEndDateTime = LocalDateTime.of(bar.actionDay(), bar.actionTime());
		if(contract.usingStartTimeInBarTime()) {
			LocalDateTime barStartDateTime = barEndDateTime;
			barEndDateTime = barStartDateTime.plusMinutes(bar.interval().getMinutes());
		}
		if(Objects.isNull(cutoffDateTime)) {			
			try {
				cutoffDateTime = barTimePadding.cutoffTimeInMinute(bar);
			} catch (IllegalStateException ex) {
				log.debug("该数据被忽略，原因：{}", ex.getMessage());
				return;
			}
		}
		
		if(bar.actionTimestamp() < curBarTimestamp) {
			if(log.isTraceEnabled()) {				
				log.trace("忽略过时数据：{} {} {} {} 当前计算时间：{}", bar.contract().name(), bar.actionDay(), bar.actionTime(), 
						bar.actionTimestamp(), LocalDateTime.ofInstant(Instant.ofEpochMilli(curBarTimestamp), ZoneId.systemDefault()));
			}
			return;
		}
		curBarTimestamp = bar.actionTimestamp();
		
		if(Objects.nonNull(protoBar) && !barEndDateTime.isAfter(cutoffDateTime)) {
			doMerge(bar);
		}
		
		if(Objects.nonNull(protoBar) && !barEndDateTime.isBefore(cutoffDateTime)) {
			doGenerate();
			if(barEndDateTime.isEqual(cutoffDateTime)) {	
				cutoffDateTime = null;
				return;
			}
		}
		
		if(Objects.isNull(protoBar)) {
			cutoffDateTime = barTimePadding.cutoffTimeInMinute(bar);
			high = bar.highPrice();
			low = bar.lowPrice();
			barVolDelta += bar.volumeDelta();
			barTurnoverDelta += bar.turnoverDelta();
			barOiDelta += bar.openInterestDelta();
			LocalDate actionDay = contract.usingStartTimeInBarTime() ? bar.actionDay() : cutoffDateTime.toLocalDate();
			LocalTime actionTime = contract.usingStartTimeInBarTime() ? bar.actionTime() : cutoffDateTime.toLocalTime();
			
			protoBar = Bar.builder()
					.gatewayId(bar.gatewayId())
					.contract(contract)
					.actionDay(actionDay)
					.actionTime(actionTime)
					.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(actionDay, actionTime)))
					.tradingDay(bar.tradingDay())
					.channelType(bar.channelType())
					.openPrice(bar.openPrice())
					.highPrice(high)
					.lowPrice(low)
					.closePrice(bar.closePrice())
					.volume(bar.volume())
					.volumeDelta(bar.volumeDelta())
					.openInterest(bar.openInterest())
					.openInterestDelta(bar.openInterestDelta())
					.turnover(bar.turnover())
					.turnoverDelta(bar.turnoverDelta())
					.preClosePrice(bar.preClosePrice())
					.preOpenInterest(bar.preOpenInterest())
					.preSettlePrice(bar.preSettlePrice())
					.interval(barInterval)
					.build();
			
			if(barEndDateTime.isEqual(cutoffDateTime)){
				doGenerate();
			}
		}
	}
	
	public synchronized void addListener(MergedBarListener listener) {
		listeners.add(listener);
	}
	
	protected void doMerge(Bar bar) {
		high = Math.max(high, bar.highPrice());
		low = Math.min(low, bar.lowPrice());
		barVolDelta += bar.volumeDelta();
		barTurnoverDelta += bar.turnoverDelta();
		barOiDelta += bar.openInterestDelta();
		
        protoBar = protoBar.toBuilder()
				.closePrice(bar.closePrice())
				.highPrice(high)
				.lowPrice(low)
				.volume(bar.volume())
				.volumeDelta(Math.max(barVolDelta, volDelta))
				.openInterest(bar.openInterest())
				.openInterestDelta(Math.max(barOiDelta, oiDelta))
				.turnover(bar.turnover())
				.turnoverDelta(Math.max(barTurnoverDelta, turnoverDelta))
				.build();
	}
	
	protected void doGenerate() {
		listeners.forEach(listener -> {
			try {
				listener.onMergedBar(protoBar.toBuilder().unstable(false).build());
			} catch (Exception e) {
				log.error("回调异常", e);
			}
		});
		protoBar = null;
		resetVal();
	}
	
	private void resetVal() {
		volDelta = 0;
		oiDelta = 0;
		turnoverDelta = 0;
		barVolDelta = 0;
		barOiDelta = 0;
		barTurnoverDelta = 0;
		high = Double.MIN_VALUE;
		low = Double.MAX_VALUE;
	}
	
	@Override
	public void onTick(Tick tick) {
		if(!contract.equals(tick.contract()) || tick.type() == TickType.INFO_TICK || tick.actionTimestamp() < curBarTimestamp) {
			return;
		}
		if(protoBar == null) {
			resetVal();
			try {
				cutoffDateTime = barTimePadding.cutoffTimeInMinute(tick);
			} catch(IllegalStateException ex) {
				log.debug("该数据被忽略，原因：{}", ex.getMessage());
				return;
			}
			
			LocalDate actionDay = contract.usingStartTimeInBarTime() ? tick.actionDay() : cutoffDateTime.toLocalDate();
			LocalTime actionTime = contract.usingStartTimeInBarTime() ? tick.actionTime() : cutoffDateTime.toLocalTime();
			
			protoBar = Bar.builder()
					.gatewayId(tick.gatewayId())
					.contract(contract)
					.actionDay(actionDay)
					.actionTime(actionTime)
					.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(actionDay, actionTime)))
					.tradingDay(tick.tradingDay())
					.channelType(tick.channelType())
					.openPrice(tick.lastPrice())
					.highPrice(tick.lastPrice())
					.lowPrice(tick.lastPrice())
					.closePrice(tick.lastPrice())
					.volume(tick.volume())
					.openInterest(tick.openInterest())
					.turnover(tick.turnover())
					.preClosePrice(tick.preClosePrice())
					.preOpenInterest(tick.preOpenInterest())
					.preSettlePrice(tick.preSettlePrice())
					.interval(barInterval)
					.build();
		}
		
		oiDelta += tick.openInterestDelta();
		turnoverDelta += tick.turnoverDelta();
		volDelta += tick.volumeDelta();
		
		high = Math.max(high, tick.lastPrice());
		low = Math.min(low, tick.lastPrice());
		
		Bar tempBar = protoBar.toBuilder()
				.closePrice(tick.lastPrice())
				.highPrice(high)
				.lowPrice(low)
				.volume(tick.volume())
				.volumeDelta(volDelta)
				.openInterest(tick.openInterest())
				.openInterestDelta(oiDelta)
				.turnover(tick.turnover())
				.turnoverDelta(turnoverDelta)
				.unstable(true)
				.build();	
        
        listeners.forEach(listener -> listener.onMergedBar(tempBar));
        
	}
	
	public Bar getCurrent() {
		return protoBar;
	}
	
}
