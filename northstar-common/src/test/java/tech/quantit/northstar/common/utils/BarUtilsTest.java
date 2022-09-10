package tech.quantit.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.DateTimeConstant;
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

}
