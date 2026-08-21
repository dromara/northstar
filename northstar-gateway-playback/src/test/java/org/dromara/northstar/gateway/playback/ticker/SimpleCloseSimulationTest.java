package org.dromara.northstar.gateway.playback.ticker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.CommonUtils;
import org.junit.jupiter.api.Test;

class SimpleCloseSimulationTest {

	SimpleCloseSimulation scs = new SimpleCloseSimulation();

	ContractDefinition cd = ContractDefinition.builder()
			.tradeTimeDef(TradeTimeDefinition.builder()
					.timeSlots(List.of(TimeSlot.builder()
							.start(LocalTime.of(9, 0))
							.end(LocalTime.of(15, 0))
							.build()))
					.build())
			.build();

	// 以结束时间作为BAR时间的合约
	Contract c = Contract.builder()
			.priceTick(1)
			.contractDefinition(cd)
			.build();

	// 以开始时间作为BAR时间的合约
	Contract c2 = Contract.builder()
			.priceTick(1)
			.contractDefinition(cd)
			.usingStartTimeInBarTime(true)
			.build();

	private Bar makeBar(Contract contract, LocalTime barTime) {
		LocalDate date = LocalDate.of(2024, 12, 2);
		return Bar.builder()
				.contract(contract)
				.actionDay(date)
				.actionTime(barTime)
				.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(date, barTime)))
				.openPrice(5000)
				.highPrice(5005)
				.lowPrice(4998)
				.closePrice(5002)
				.openInterestDelta(31)
				.volumeDelta(2000)
				.build();
	}

	@Test
	void testTickTimeWithinBarPeriodForEndTimeContract() {
		// 以结束时间作为BAR时间：09:01 的K线代表 09:00-09:01 的数据
		Bar bar = makeBar(c, LocalTime.of(9, 1));
		List<TickEntry> ticks = scs.generateFrom(bar);
		assertThat(ticks).hasSize(1);
		long tickTime = ticks.getFirst().timestamp();
		// TICK时间戳必须落在该K线对应的分钟区间内，不能恰好等于K线结束时刻（否则会被归入下一分钟K线）
		assertThat(tickTime).isGreaterThanOrEqualTo(bar.actionTimestamp() - 60000);
		assertThat(tickTime).isLessThan(bar.actionTimestamp());
	}

	@Test
	void testTickTimeWithinBarPeriodForStartTimeContract() {
		// 以开始时间作为BAR时间：09:00 的K线代表 09:00-09:01 的数据
		Bar bar = makeBar(c2, LocalTime.of(9, 0));
		List<TickEntry> ticks = scs.generateFrom(bar);
		assertThat(ticks).hasSize(1);
		long tickTime = ticks.getFirst().timestamp();
		assertThat(tickTime).isGreaterThanOrEqualTo(bar.actionTimestamp());
		assertThat(tickTime).isLessThan(bar.actionTimestamp() + 60000);
	}

	@Test
	void testSkipStartingBarForEndTimeContract() {
		// 开盘第一节BAR不生成TICK（避免产生交易时段外的TICK）
		Bar bar = makeBar(c, LocalTime.of(9, 0));
		assertThat(scs.generateFrom(bar)).isEmpty();
	}
}
