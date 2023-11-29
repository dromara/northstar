package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.function.Consumer;

import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MinuteBarGenerator {
	
	private static final LocalTime MIDNIGHT = LocalTime.of(0, 0);

	private LocalTime cutoffTime;
	
	private Contract contract;
	
	private Tick lastTick;
	
	private Consumer<Bar> onBarCallback;
	
	private Bar proto;
	
	private double high;
	private double low;
	private double close;
	private double open;
	private long volumeDelta;
	private double openInterestDelta;
	private double turnoverDelta;
	
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
			return;
		}
		
		lastTick = tick;
		
		if(Objects.nonNull(cutoffTime) && tick.actionTime().isAfter(cutoffTime)) {
			finishOfBar();
		}
		if(Objects.isNull(proto)) {
			cutoffTime = tick.actionTime().withSecond(0).withNano(0).plusMinutes(1);
			open = tick.lastPrice();
			high = tick.lastPrice();
			low = tick.lastPrice();
			close = tick.lastPrice();
			openInterestDelta = 0;
			volumeDelta = 0;
			turnoverDelta = 0;
			proto = Bar.builder()
					.gateway(tick.gateway())
					.channelType(tick.channelType())
					.contract(tick.contract())
					.actionDay(MIDNIGHT.equals(cutoffTime) ? tick.actionDay().plusDays(1) : tick.actionDay())
					.actionTime(cutoffTime)
					.tradingDay(tick.tradingDay())
					.build();
		}
		high = Math.max(tick.lastPrice(), high);
		low = Math.min(tick.lastPrice(), low);
		close = tick.lastPrice();
		openInterestDelta += tick.openInterestDelta();
		volumeDelta += tick.volumeDelta();
		turnoverDelta += tick.turnoverDelta();
	}
	
	/**
	 * 分钟收盘生成
	 * @return
	 */
	public synchronized void finishOfBar() {
		if(Objects.isNull(proto)) {
			return;
		}
		onBarCallback.accept(Bar.builder()
				.gateway(proto.gateway())
				.channelType(proto.channelType())
				.contract(proto.contract())
				.actionDay(proto.actionDay())
				.actionTime(proto.actionTime())
				.actionTimestamp(LocalDateTime.of(proto.actionDay(), proto.actionTime()).toInstant(ZoneOffset.of(ZoneOffset.systemDefault().toString())).toEpochMilli())
				.tradingDay(proto.tradingDay())
				.openPrice(open)
				.highPrice(high)
				.lowPrice(low)
				.closePrice(close)
				.preClosePrice(lastTick.preClosePrice())
				.preOpenInterest(lastTick.preOpenInterest())
				.preSettlePrice(lastTick.preSettlePrice())
				.volume(lastTick.volume())
				.volumeDelta(Math.max(0, volumeDelta))
				.openInterest(lastTick.openInterest())
				.openInterestDelta(openInterestDelta)
				.turnover(lastTick.turnover())
				.turnoverDelta(turnoverDelta)
				.build());
		proto = null;
		cutoffTime = null;
	}
	
}
