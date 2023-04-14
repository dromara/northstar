package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.utils.BarUtils;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreField.BarField;

class BarUtilsTest {

	@Test
	void test() {
		List<BarField> samples = new ArrayList<>();
		
		LocalDate startDate = LocalDate.of(2022, 7, 5);
		LocalDate endDate = LocalDate.of(2022, 9, 10);
		LocalDate date = startDate;
		while(!date.isAfter(endDate)) {
			samples.add(BarField.newBuilder()
					.setUnifiedSymbol(date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.setActionDay(date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.setTradingDay(date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.setActionTime("15:00:00")
					.build());
			date = date.plusDays(1);
		}
		
		assertThat(BarUtils.mergeWeeklyBar(samples)).hasSize(10);
	}

	@Test
	void testEndTime() {
		BarField.Builder bb = BarField.newBuilder().setActionTime("15:00").setGatewayId("CTP");
		
		assertThat(BarUtils.isEndOfTheTradingDay(bb.setUnifiedSymbol("TA2210@SHFE@FUTURES").build())).isTrue();
		assertThat(BarUtils.isEndOfTheTradingDay(bb.setUnifiedSymbol("TS@CFFEX@FUTURES").build())).isFalse();
		assertThat(BarUtils.isEndOfTheTradingDay(bb.setUnifiedSymbol("T@CFFEX@FUTURES").build())).isFalse();
		assertThat(BarUtils.isEndOfTheTradingDay(bb.setUnifiedSymbol("TF@CFFEX@FUTURES").build())).isFalse();
	}
	
	@Test
	void testEndTime2() {
		BarField.Builder bb = BarField.newBuilder().setActionTime("15:15").setGatewayId("CTP");
		
		assertThat(BarUtils.isEndOfTheTradingDay(bb.setUnifiedSymbol("TA2210@SHFE@FUTURES").build())).isTrue();
		assertThat(BarUtils.isEndOfTheTradingDay(bb.setUnifiedSymbol("TS@CFFEX@FUTURES").build())).isTrue();
		assertThat(BarUtils.isEndOfTheTradingDay(bb.setUnifiedSymbol("T@CFFEX@FUTURES").build())).isTrue();
		assertThat(BarUtils.isEndOfTheTradingDay(bb.setUnifiedSymbol("TF@CFFEX@FUTURES").build())).isTrue();
	}
}
