package org.dromara.northstar.gateway.mktdata;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.DateTimeUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MinuteBarGenerator {
	
	private static final LocalTime MIDNIGHT = DateTimeUtils.fromCacheTime(0, 0);
	private static final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
	
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
		// 如果超过120秒内没有tick更新，则自动合成K线
		exec.scheduleAtFixedRate(() -> {
			if(proto != null && lastTick != null && System.currentTimeMillis() - lastTick.actionTimestamp() > 120000) {
				log.trace("强制K线收盘：{}", contract.name());
				finishOfBar();
			}
		}, 60, 60, TimeUnit.MINUTES);
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
		
		if(Objects.nonNull(cutoffTime) && !tick.actionTime().isBefore(cutoffTime)) {
			finishOfBar();
		}
		if(Objects.isNull(proto)) {
			cutoffTime = DateTimeUtils.fromCacheTime(tick.actionTime().withSecond(0).withNano(0).plusMinutes(1));
			open = tick.lastPrice();
			high = tick.lastPrice();
			low = tick.lastPrice();
			close = tick.lastPrice();
			openInterestDelta = 0;
			volumeDelta = 0;
			turnoverDelta = 0;
			proto = Bar.builder()
					.gatewayId(tick.gatewayId())
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
				.gatewayId(proto.gatewayId())
				.channelType(proto.channelType())
				.contract(proto.contract())
				.actionDay(proto.actionDay())
				.actionTime(proto.actionTime())
				.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(proto.actionDay(), proto.actionTime())))
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
