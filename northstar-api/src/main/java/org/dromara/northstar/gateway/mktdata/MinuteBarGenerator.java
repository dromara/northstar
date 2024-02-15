package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MinuteBarGenerator {
	
	private Long cutoffTime;
	private LocalDateTime cutoffDT;
	
	private Contract contract;
	
	private Tick curTick;
	
	private Consumer<Bar> onBarCallback;
	
	private double high;
	private double low;
	private double close;
	private double open;
	private long volumeDelta;
	private double openInterestDelta;
	private double turnoverDelta;
	
	private CompletableFuture<Void> asyncCheck;
	
	private Runnable forceCloseBar = () -> {
		synchronized(MinuteBarGenerator.this) {			
			if(curTick != null && System.currentTimeMillis() - curTick.actionTimestamp() > TimeUnit.MINUTES.toMillis(1)) {
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
			// cutoffTime是下一个整数分钟前推100毫秒
			cutoffTime = CommonUtils.localDateTimeToMills(cutoffDT) - 100;
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
		// 1分钟后检查
		asyncCheck = CompletableFuture.runAsync(forceCloseBar, CompletableFuture.delayedExecutor(1, TimeUnit.MINUTES));
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
				.build());
		cutoffTime = null;
	}
	
}
