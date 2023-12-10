package org.dromara.northstar.indicator;

import java.util.Objects;
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

	private Bar barProto;
	private final Contract contract;
	
	public InstantBarGenerator(Contract contract) {
		this.contract = contract;
	}

	public synchronized Optional<Bar> update(Tick tick) {
		if (!contract.equals(tick.contract())) {
			throw new IllegalArgumentException("合约不匹配，期望合约：" + contract.unifiedSymbol() + "，实际合约：" + tick.contract().unifiedSymbol());
		}

		if(Objects.nonNull(barProto) && tick.actionTimestamp() % 60000 == 0) {
			barProto = null;
			return Optional.empty();
		}
		
		if(barProto == null) {
			barProto = Bar.builder()
					.actionDay(tick.actionDay())
					.actionTime(tick.actionTime())
					.actionTimestamp(tick.actionTimestamp())
					.tradingDay(tick.tradingDay())
					.openPrice(tick.lastPrice())
					.highPrice(tick.lastPrice())
					.lowPrice(tick.lastPrice())
					.closePrice(tick.lastPrice())
					.contract(tick.contract())
					.gatewayId(tick.gatewayId())
					.build();
		}
		
		barProto = barProto.toBuilder()
				.highPrice(Math.max(barProto.highPrice(), tick.lastPrice()))
				.lowPrice(Math.min(barProto.lowPrice(), tick.lastPrice()))
				.closePrice(tick.lastPrice())
				.openInterest(tick.openInterest())
				.volume(tick.volume())
				.volumeDelta(barProto.volumeDelta() + tick.volumeDelta())
				.turnover(tick.turnover())
				.turnoverDelta(barProto.turnoverDelta() + tick.turnoverDelta())
				.openInterest(tick.openInterest())
				.openInterestDelta(barProto.openInterestDelta() + tick.openInterestDelta())
				.build();
		return Optional.of(barProto);
	}
	
}
