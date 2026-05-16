package org.dromara.northstar.kline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.CommonUtils;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

class BarTimePaddingTest {

	Contract contract = Contract.builder()
			.symbol("TEST")
			.exchange(ExchangeEnum.SHFE)
			.contractDefinition(ContractDefinition.builder()
					.tradeTimeDef(TradeTimeDefinition.builder()
							.timeSlots(List.of(
									TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(10, 15)).build(),
									TimeSlot.builder().start(LocalTime.of(10, 30)).end(LocalTime.of(11, 30)).build(),
									TimeSlot.builder().start(LocalTime.of(13, 30)).end(LocalTime.of(15, 0)).build()
							))
							.build())
					.build())
			.build();
	
	@Test
	void test1() {
		BarTimePadding padding = new BarTimePadding(Interval.H1, contract);
		LocalDateTime ldt1 = LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 25));
		LocalDateTime expDt1 = LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 15));
		assertTrue(padding.cutoffTimeInMinute(CommonUtils.localDateTimeToMills(ldt1)).isEqual(expDt1));
	}
	
	@Test
	void test2() {
		BarTimePadding padding = new BarTimePadding(Interval.H4, contract);
		LocalDateTime ldt1 = LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 25));
		LocalDateTime expDt1 = LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 00));
		assertTrue(padding.cutoffTimeInMinute(CommonUtils.localDateTimeToMills(ldt1)).isEqual(expDt1));
	}
	
}
