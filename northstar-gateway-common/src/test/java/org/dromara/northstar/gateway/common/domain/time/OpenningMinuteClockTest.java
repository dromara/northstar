package org.dromara.northstar.gateway.common.domain.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.TradeTimeDefinition;
import org.dromara.northstar.gateway.common.domain.time.OpenningMinuteClock;
import org.dromara.northstar.gateway.model.PeriodSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.TickField;

class OpenningMinuteClockTest {

	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	TradeTimeDefinition ttd = new TradeTimeDefinition() {
		
		@Override
		public List<PeriodSegment> tradeTimeSegments() {
			return List.of(new PeriodSegment(LocalTime.of(21, 0), LocalTime.of(23, 00)),
					new PeriodSegment(LocalTime.of(9, 1), LocalTime.of(10, 15)),
					new PeriodSegment(LocalTime.of(10, 31), LocalTime.of(11, 30)),
					new PeriodSegment(LocalTime.of(13, 31), LocalTime.of(15, 00)));
		}
	};
	
	OpenningMinuteClock clock;
	
	@BeforeEach
	void prepare() {
		clock = new OpenningMinuteClock(ttd);
	}

	@Test
	void testBarMin() {
		TickField t1 = TickField.newBuilder()
				.setLastPrice(1000)
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime("100000000")
				.setActionTimestamp(LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 0)).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.build();
		TickField t2 = TickField.newBuilder()
				.setLastPrice(1000)
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime("095900500")
				.setActionTimestamp(LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 59, 0, 500)).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.build();
		TickField t3 = TickField.newBuilder()
				.setLastPrice(1000)
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime("100000500")
				.setActionTimestamp(LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 0, 0, 500)).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.build();
		
		assertThat(clock.barMinute(t1).toLocalTime()).isEqualTo(LocalTime.of(10, 1));
		assertThat(clock.barMinute(t2).toLocalTime()).isEqualTo(LocalTime.of(10, 0));
		assertThat(clock.barMinute(t3).toLocalTime()).isEqualTo(LocalTime.of(10, 1));
	}

}
