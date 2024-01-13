package org.dromara.northstar.indicator;

import java.util.Optional;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;

/**
 * 即时K线生成器
 * 根据TICK数据生成即时K线 
 * @author KevinHuangwl
 *
 */
public class InstantBarGenerator {

	private final Contract contract;
	
	private double openPrice;
	private double highPrice;
	private double lowPrice;
	private long volDelta;
	private long turnoverDelta;
	private double openInterestDelta;
	
	public InstantBarGenerator(Contract contract) {
		this.contract = contract;
	}

	public synchronized Optional<Bar> update(Tick tick) {
		if (!contract.equals(tick.contract())) {
			throw new IllegalArgumentException("合约不匹配，期望合约：" + contract.unifiedSymbol() + "，实际合约：" + tick.contract().unifiedSymbol());
		}

		if(tick.actionTimestamp() % 60000 == 0) {
			openPrice = tick.lastPrice();
			highPrice = openPrice;
			lowPrice = openPrice;
			volDelta = 0;
			turnoverDelta = 0;
			openInterestDelta = 0;
			return Optional.empty();
		}
		
		highPrice = Math.max(highPrice, tick.lastPrice());
		lowPrice = Math.min(lowPrice, tick.lastPrice());
		volDelta += tick.volumeDelta();
		turnoverDelta += tick.turnoverDelta();
		openInterestDelta += tick.openInterestDelta();
		
		return Optional.of(Bar.builder()
				.actionDay(tick.actionDay())
				.actionTime(tick.actionTime())
				.actionTimestamp(tick.actionTimestamp())
				.tradingDay(tick.tradingDay())
				.gatewayId(tick.gatewayId())
				.contract(tick.contract())
				.openPrice(openPrice)
				.highPrice(highPrice)
				.lowPrice(lowPrice)
				.closePrice(tick.lastPrice())
				.volume(tick.volume())
				.volumeDelta(volDelta)
				.turnover(tick.turnover())
				.turnoverDelta(turnoverDelta)
				.openInterest(tick.openInterest())
				.openInterestDelta(openInterestDelta)
				.build());
	}
	
}
