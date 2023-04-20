package org.dromara.northstar.gateway.common.domain.mktdata;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.function.Consumer;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.common.domain.time.OpenningMinuteClock;
import org.dromara.northstar.gateway.common.domain.time.TradeTimeDefinition;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class MinuteBarGenerator {

	private BarField.Builder barBuilder;

	private static final long MAX_TIME_GAP = 90000; //90秒TICK过期判定

	private LocalDateTime cutoffTime;
	
	private ContractField contract;
	
	private OpenningMinuteClock clock;
	
	private TickField lastTick;
	
	Consumer<BarField> onBarCallback;
	
	public MinuteBarGenerator(ContractField contract, TradeTimeDefinition tradeTimeDefinition, Consumer<BarField> onBarCallback) {
		this.contract = contract;
		this.onBarCallback = onBarCallback;
		this.clock = new OpenningMinuteClock(tradeTimeDefinition);
	}
	
	/**
	 * 更新Tick数据
	 * 
	 * @param tick
	 */
	public synchronized void update(TickField tick) {
		// 如果tick为空或者合约不匹配则返回
		if (tick == null || !contract.getUnifiedSymbol().equals(tick.getUnifiedSymbol())) {
			log.warn("合约不匹配,当前Bar合约{}", contract.getUnifiedSymbol());
			return;
		}
		if (System.currentTimeMillis() - tick.getActionTimestamp() > MAX_TIME_GAP) {
			log.debug("忽略过期数据: {} {} {}", tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime());
			return;
		}
		
		// 忽略非行情数据
		if(tick.getStatus() < 1 && !clock.isValidOpenningTick(tick)) {
			return;
		}
		
		lastTick = tick;
		
		if(Objects.nonNull(cutoffTime) && !clock.isEndOfSection(cutoffTime.toLocalTime()) && tick.getActionTimestamp() >= cutoffTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli()) {
			finishOfBar();
		}
		if(Objects.isNull(barBuilder)) {
			cutoffTime = clock.barMinute(tick);
			barBuilder = BarField.newBuilder()
					.setGatewayId(contract.getGatewayId())
					.setUnifiedSymbol(contract.getUnifiedSymbol())
					.setTradingDay(tick.getTradingDay())
					.setOpenPrice(tick.getLastPrice())
					.setHighPrice(tick.getLastPrice())
					.setLowPrice(tick.getLastPrice())
					.setPreClosePrice(tick.getPreClosePrice())
					.setPreOpenInterest(tick.getPreOpenInterest())
					.setPreSettlePrice(tick.getPreSettlePrice())
					.setActionDay(tick.getActionDay())
					.setActionTime(cutoffTime.format(DateTimeConstant.T_FORMAT_FORMATTER))
					.setActionTimestamp(cutoffTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
		}
		
		barBuilder.setHighPrice(Math.max(tick.getLastPrice(), barBuilder.getHighPrice()));
		barBuilder.setLowPrice(Math.min(tick.getLastPrice(), barBuilder.getLowPrice()));
		barBuilder.setClosePrice(tick.getLastPrice());
		barBuilder.setOpenInterest(tick.getOpenInterest());
		barBuilder.setOpenInterestDelta(tick.getOpenInterestDelta() + barBuilder.getOpenInterestDelta());
		barBuilder.setVolume(tick.getVolumeDelta() + barBuilder.getVolume());
		barBuilder.setTurnover(tick.getTurnoverDelta() + barBuilder.getTurnover());
		barBuilder.setNumTrades(tick.getNumTradesDelta() + barBuilder.getNumTrades());
	}
	
	/**
	 * 分钟收盘生成
	 * @return
	 */
	public synchronized BarField finishOfBar() {
		if(Objects.isNull(barBuilder)) {
			return null;
		}
		barBuilder.setVolume(Math.max(0, barBuilder.getVolume()));				// 防止vol为负数
		
		BarField lastBar = barBuilder.build();
		onBarCallback.accept(lastBar);
		barBuilder = null;
		cutoffTime = null;
		return lastBar;
	}
	
	/**
	 * 小节收盘检查
	 */
	public synchronized void endOfBar() {
		if(Objects.isNull(lastTick) || System.currentTimeMillis() - lastTick.getActionTimestamp() < MAX_TIME_GAP) {
			return;
		}
		finishOfBar();
	}
	
}
