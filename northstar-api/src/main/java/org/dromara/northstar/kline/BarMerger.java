package org.dromara.northstar.kline;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
	
	private MinuteBarGenerator minBarGen;
	
	public BarMerger(Interval interval, Contract contract) { 
		this.contract = contract;
		this.barInterval = interval;
		this.barTimePadding = new BarTimePadding(interval, contract);
		
		if(interval == Interval.M1) {
			minBarGen = new MinuteBarGenerator(contract, this::onBar);
		}
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
		if(!contract.equals(tick.contract()) || tick.type() == TickType.INFO_TICK
				|| tick.actionTimestamp() < curBarTimestamp) {
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
        
        if(Objects.nonNull(minBarGen)) {
			minBarGen.update(tick);
        }
	}
	
	public Bar getCurrent() {
		return protoBar;
	}
	
	private static class MinuteBarGenerator {
		
		private Long cutoffTime;
		private LocalDateTime cutoffDT;
		
		private Contract contract;
		
		private Tick curTick;
		
		private Consumer<Bar> onBarCallback;
		
		private double high;
		private double low;
		private double close;
		private double open;
		private int volumeDelta;
		private int openInterestDelta;
		private double turnoverDelta;
		
		private CompletableFuture<Void> asyncCheck;
		
		private Runnable forceCloseBar = () -> {
			synchronized(MinuteBarGenerator.this) {			
				if(curTick != null && System.currentTimeMillis() - curTick.actionTimestamp() > TimeUnit.MINUTES.toMillis(2)) {
					log.debug("强制K线收盘：{}", contract.name());
					finishOfBar();
				}
			}
		};
		
		/**
		 * 用于实盘数据
		 * @param contract
		 * @param tradeTimeDefinition
		 * @param onBarCallback
		 */
		public MinuteBarGenerator(Contract contract, Consumer<Bar> onBarCallback) {
			this.contract = contract;
			this.onBarCallback = onBarCallback;
		}
		
		/**
		 * 更新Tick数据
		 * 
		 * @param tick
		 */
		public synchronized void update(Tick tick) {
			// 如果tick为空或者合约不匹配则返回
			if (tick == null) {
				log.trace("TICK数据为空，将被忽略");
				return;
			}
			boolean sameSymbol = contract.equals(tick.contract());
			boolean sameChannel = contract.channelType() == tick.channelType();
			if(!(sameSymbol && sameChannel)) {
				if(!sameSymbol)	log.warn("合约不匹配，期望 [{}]，实际 [{}]", contract.contractId(), tick.contract().contractId());
				if(!sameChannel) log.warn("[{}] 合约渠道不匹配，期望 [{}]，实际 [{}]", contract.unifiedSymbol(), contract.channelType(), tick.channelType());
				return;
			}
			// 忽略非行情数据
			if(tick.type() != TickType.MARKET_TICK) {
				log.trace("忽略非行情数据：{}", tick);
				return;
			}
			
			curTick = tick;
			
			if(Objects.nonNull(cutoffTime) && cutoffTime < tick.actionTimestamp()) {
				finishOfBar();
			}
			if(Objects.isNull(cutoffTime)) {
				cutoffDT = LocalDateTime.of(tick.actionDay(), tick.actionTime().withSecond(0).withNano(0)).plusMinutes(1);
				cutoffTime = CommonUtils.localDateTimeToMills(cutoffDT);
				open = tick.lastPrice();
				high = tick.lastPrice();
				low = tick.lastPrice();
				close = tick.lastPrice();
				openInterestDelta = 0;
				volumeDelta = 0;
				turnoverDelta = 0;
			}
			high = Math.max(tick.lastPrice(), high);
			low = Math.min(tick.lastPrice(), low);
			close = tick.lastPrice();
			openInterestDelta += tick.openInterestDelta();
			volumeDelta += tick.volumeDelta();
			turnoverDelta += tick.turnoverDelta();
			
			if(asyncCheck != null) {			
				asyncCheck.cancel(false);
			}
			// 2分钟后检查
			asyncCheck = CompletableFuture.runAsync(forceCloseBar, CompletableFuture.delayedExecutor(2, TimeUnit.MINUTES));
		}
		
		/**
		 * 分钟收盘生成
		 * @return
		 */
		public synchronized void finishOfBar() {
			if(Objects.isNull(cutoffTime)) {
				return;
			}
			onBarCallback.accept(Bar.builder()
					.gatewayId(curTick.gatewayId())
					.channelType(curTick.channelType())
					.contract(curTick.contract())
					.actionDay(cutoffDT.toLocalDate())
					.actionTime(cutoffDT.toLocalTime())
					.actionTimestamp(CommonUtils.localDateTimeToMills(cutoffDT))
					.tradingDay(curTick.tradingDay())
					.openPrice(open)
					.highPrice(high)
					.lowPrice(low)
					.closePrice(close)
					.preClosePrice(curTick.preClosePrice())
					.preOpenInterest(curTick.preOpenInterest())
					.preSettlePrice(curTick.preSettlePrice())
					.volume(curTick.volume())
					.volumeDelta(Math.max(0, volumeDelta))
					.openInterest(curTick.openInterest())
					.openInterestDelta(openInterestDelta)
					.turnover(curTick.turnover())
					.turnoverDelta(turnoverDelta)
					.interval(Interval.M1)
					.build());
			cutoffTime = null;
		}
		
	}
	
}
