package tech.quantit.northstar.strategy.api.utils.bar;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.gateway.api.domain.time.BarMergingClock;
import tech.quantit.northstar.gateway.api.domain.time.PeriodHelper;
import tech.quantit.northstar.strategy.api.BarDataAware;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 分钟线合成器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class BarMerger implements BarDataAware{
	
	protected Consumer<BarField> callback;
	
	protected Contract contract;
	
	protected BarField.Builder barBuilder;
	
	protected long curBarTimestamp;
	
	protected final String unifiedSymbol;
	
	private PeriodHelper phelper;
	
	private final int numOfMinPerBar;
	
	private final BarMergingClock clock;
	
	public BarMerger(int numOfMinPerBar, Contract contract, Consumer<BarField> callback) {
		this.callback = callback;
		this.contract = contract;
		this.numOfMinPerBar = numOfMinPerBar;
		this.unifiedSymbol = contract.contractField().getUnifiedSymbol();
		this.phelper = new PeriodHelper(numOfMinPerBar, contract.tradeTimeDefinition());
		this.clock = new BarMergingClock(phelper.getRunningBaseTimeFrame());
	}
	
	@Override
	public synchronized void onBar(BarField bar) {
		if(!StringUtils.equals(bar.getUnifiedSymbol(), unifiedSymbol)) {
			return;
		}
		LocalTime barTime = LocalTime.parse(bar.getActionTime(), DateTimeConstant.T_FORMAT_FORMATTER);
		if(bar.getActionTimestamp() < curBarTimestamp) {
			if(log.isTraceEnabled()) {				
				log.trace("当前计算时间：{}", LocalDateTime.ofInstant(Instant.ofEpochMilli(curBarTimestamp), ZoneId.systemDefault()));
				log.trace("忽略过时数据：{} {} {} {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getActionTimestamp());
			}
			return;
		}
		curBarTimestamp = bar.getActionTimestamp();
		
		if(numOfMinPerBar <= 1) {
			callback.accept(bar);
			return;
		}
		
		if(clock.adjustTime(barTime) && Objects.nonNull(barBuilder)) {
			doGenerate();
		}
		
		if(Objects.isNull(barBuilder)) {
			barBuilder = bar.toBuilder();
			return;
		}
		
		doMerge(bar);
		
		if(barTime.equals(clock.currentTimeBucket())) {
			doGenerate();
			clock.next();
		}
	}
	
	protected void doGenerate() {
		callback.accept(barBuilder.build());
		barBuilder = null;
	}
	
	protected void doMerge(BarField bar) {
		double high = barBuilder.getHighPrice();
		double low = barBuilder.getLowPrice();
		long vol = barBuilder.getVolume();
		long volumeDelta = barBuilder.getVolumeDelta();
		long numOfTrade = barBuilder.getNumTrades();
		long numOfTradeDelta = barBuilder.getNumTradesDelta();
		double turnover = barBuilder.getTurnover();
		double turnoverDelta = barBuilder.getTurnoverDelta();
		double openInterestDelta = barBuilder.getOpenInterestDelta();
		
		barBuilder
			.setActionDay(bar.getActionDay())
			.setActionTime(bar.getActionTime())
			.setActionTimestamp(bar.getActionTimestamp())
			.setTradingDay(bar.getTradingDay())
			.setClosePrice(bar.getClosePrice())
			.setHighPrice(Math.max(high, bar.getHighPrice()))
			.setLowPrice(Math.min(low, bar.getLowPrice()))
			.setVolume(vol + bar.getVolume())
			.setVolumeDelta(volumeDelta + bar.getVolumeDelta())
			.setOpenInterest(bar.getOpenInterest())
			.setOpenInterestDelta(openInterestDelta + bar.getOpenInterestDelta())
			.setNumTrades(numOfTrade + bar.getNumTrades())
			.setNumTradesDelta(numOfTradeDelta + bar.getNumTradesDelta())
			.setTurnover(turnover + bar.getTurnover())
			.setTurnoverDelta(turnoverDelta + bar.getTurnoverDelta());
	}

}
